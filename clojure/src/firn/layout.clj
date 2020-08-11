(ns firn.layout
  "Namespace responsible for using layouts.
  Layouts enable users to have custom layouts for the static site generators.
  This occurs by slurping in some layout files -- which are just `.clj` files
  And then applying them inline."

  (:require [firn.markup :as markup]
            [firn.org :as org]
            [hiccup.core :as h]
            [firn.file :as file]))

(defn internal-default-layout
  "The default template if no `layout` key and no default.clj layout is specified."
  [{:keys [render] :as data}]
  [:html
   [:head
    [:meta {:charset "UTF-8"}]
    [:link {:rel "stylesheet" :href "/static/css/firn_base.css"}]]
   [:main
    [:div (render :toc)]
    [:div (render :file)]]])

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
  ([partial-map action]
   (render partial-map action {}))
  ([partial-map action opts]
   (let [{:keys [file config]} partial-map
         org-tree              (file :as-edn)
         config-settings       (config :user-config)     ; site-wide config: 0 precedence
         site-map              (config :site-map)
         file-settings         (file/keywords->map file) ; file-setting config: 2 precedence
         layout-settings       (if (map? opts) opts {})
         merged-options        (merge config-settings layout-settings file-settings)
         cached-sitemap-html   (atom nil)
         is-headline?          (string? action)]

     ;; cache the site-map if it's not there already
     (when-not @cached-sitemap-html
       (reset! cached-sitemap-html (markup/render-site-map site-map opts)))

     (cond
       ;; render the whole file.
       (= action :file)
       (markup/to-html (file :as-edn) merged-options)

       ;; render just the content of a headline.
       (and is-headline? (opts :exclude-headline?))
       (let [headline-content (org/get-headline-content org-tree action)]
         (markup/to-html headline-content merged-options))

       ;; render a heading (title and content).
       (and is-headline?)
       (markup/to-html (org/get-headline org-tree action) merged-options)

       ;; render a polyline graph of the logbook of the file.
       (= action :logbook-polyline)
       (org/poly-line (-> file :meta :logbook) opts)

       ;; Render the sitemap; cache it the first time it runs
       (= action :sitemap)
       (if-not @cached-sitemap-html
         (do (reset! cached-sitemap-html (markup/render-site-map site-map opts))
             @cached-sitemap-html)
         @cached-sitemap-html)

       ;; render breadcrumbs
       (= action :breadcrumbs)
       (markup/render-breadcrumbs (-> file :meta :firn-under) site-map opts)

       ;; render a table of contents
       (= action :toc)
       (let [toc  (-> file :meta :toc) ; get the toc for the file.
             ;; get configuration for toc in order of precedence
             opts (merge (config-settings :firn-toc)
                         layout-settings
                         (file-settings :firn-toc))]
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

(defn build-url
  "returns a helper function for use in layouts for easier building of urls from
  site-url"
  [site-url]
  (fn [& args] (apply str site-url args)))

(defn prepare
  "Prepare functions and data to be available in layout functions.
  This is a 'public api' that a user would 'invoke' for common rendering tasks
  made available in user layouts.
  NOTE | PERF:  This might be being called twice."
  [config file]
  (let [site-url (-> config :user-config :site-url)]
    {;; Layout stuff --
     :render        (partial render {:file file :config config})
     :partials      (config :partials)
     ;; Site-side stuff --
     :site-map      (config :site-map)
     :site-links    (config :site-links)
     :site-logs     (config :site-logs)
     :site-url      site-url
     :build-url     (build-url site-url)
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
     :date-created  (-> file :meta :date-created)}))

(defn apply-layout
  "If a file has a template, render the file with it, or use the default layout"
  [config file layout]
  (let [selected-layout (get-layout config file layout)]
    (h/html (selected-layout (prepare config file)))))

