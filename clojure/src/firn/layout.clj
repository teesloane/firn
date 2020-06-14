(ns firn.layout
  "Namespace responsible for using layouts.
  Layouts enable users to have custom layouts for the static site generators.
  This occurs by slurping in some layout files -- which are just `.clj` files
  And then applying them inline."

  (:require [firn.markup :as markup]
            [firn.org :as org]
            [hiccup.core :as h]
            [sci.core :as sci]))

(defn- internal-default-layout
  "The default template if no `layout` key is specified.
  This lets users know they need to build a `_layouts/default.clj`"
  [{:keys [curr-file]}]
  [:main
   [:div (markup/to-html (:as-edn curr-file))]])

(defn get-layout
  "Checks if a layout for a project exists in the config map
  If it does, return the function value of the layout, otherwise the default template "
  [config file layout]
  (let [curr-file-name (file :name)
        file-layout    (get-in config [:layouts layout])
        default-layout (-> config :layouts :default)]
    (cond
      (not (nil? file-layout))
      file-layout

      (not (nil? default-layout))
      default-layout

      :else
      (do
        (if layout
          (println "\n⚠ File:" curr-file-name "says it uses a layout of" layout "but no corresponding layout file exists in _firn/layouts")
          (println "\n⚠ File:" curr-file-name "does not have #+FIRN_LAYOUT key and no default layout file was found."))
        (println "☝ Resorting to internal template!\n")
        internal-default-layout))))

(defn render
  "Renders something from your org-mode file.
  This would be a nice multi-method if we could find a way
  to partially apply the file map to it."
  ([file action]
   (render file action {}))
  ([file action opts]
   (let [org-tree     (file :as-edn)
         is-headline? (string? action)]
     (cond
       ;; render the whole file.
       (= action :file)
       (markup/to-html (file :as-edn))

       ;; render a headline title.
       (and is-headline? (= opts :title))
       (let [hl (org/get-headline org-tree action)]
         (-> hl :children first  markup/to-html))

       ;; render the headline raw.
       (and is-headline? (= opts :title-raw))
       (let [hl (org/get-headline org-tree action)]
         (-> hl :children first :raw))

       ;; render just the content of a headline.
       (and is-headline? (= opts :content))
       (let [headline-content (org/get-headline-content org-tree action)]
         (markup/to-html headline-content))

       ;; render a heading (title and contnet).
       (and is-headline? (= nil action))
       (markup/to-html (org/get-headline org-tree action))

       ;; render a polyline graph of the logbook of the file.
       (= action :logbook-polyline)
       (org/poly-line (-> file :meta :logbook) opts)

       ;; render a table of contents
       (= action :toc)
       (let [toc      (-> file :meta :toc) ; get the toc for hte file.
             firn_toc (sci/eval-string (org/get-keyword file "FIRN_TOC")) ; read in keyword for overrides
             opts     (or firn_toc opts {})] ; apply most pertinent options.
         (when (seq toc)
            (markup/make-toc toc opts)))

       :else ; error message to indicate incorrect use of render.
       (str "<div style='position: fixed; background: antiquewhite; z-index: 999; padding: 24px; left: 33%; top: 33%; border: 13px solid lightcoral; box-shadow: 3px 3px 3px rgba(0, 0, 0, 0.3);'>"
            "<div style='text-align: center'>Render Error.</div>"
            "<div>Incorrect use of `render` function in template:
                <br> action: => " action " <code> << is this a valid value? </code>
                <br> opts:  => " opts " <code> << is this a valid value? </code>"
            "<br></div> "
            "</div>")))))

(defn prepare
  "Prepare functions and data to be available in layout functions.
  NOTE: pretty sure this being called twice as well. Do PERF work."
  [config file]
  {;; Layout stuff --
   :render        (partial render file)
   :partials      (config :partials)
   ;; Site-side stuff --
   :site-map      (config :site-map)
   :site-links    (config :site-links)
   :site-logs     (config :site-logs)
   :config        config
   ;; File wide meta --
   :file          file
   :meta          (file :meta)
   :logbook       (-> file :meta :logbook)
   :file-links    (-> file :meta :links)
   :title         (-> file :meta :title)
   :firn-under    (-> file :meta :firn-under)
   :logbook-total (-> file :meta :logbook-total)
   :date-updated  (-> file :meta :date-updated)
   :date-created  (-> file :meta :date-created)})

(defn apply-layout
  "If a file has a template, render the file with it, or use the default layout"
  [config file layout]
  (let [selected-layout (get-layout config file layout)]
    (h/html (selected-layout (prepare config file)))))

