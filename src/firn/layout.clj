(ns firn.layout
  "Namespace responsible for using layouts.
  Layouts enable users to have custom layouts for the static site generators.
  This occurs by slurping in some layout files -- which are just `.clj` files, for now
  And then applying them inline.

  NOTE: will change (apply-templates, especially) in the future:
  ; a) probably can't compile down with GRAAL and
  ; b) eval is not a good idea, probably."
  (:require [firn.markup :as markup]
            [firn.org :as org]
            [hiccup.core :as h]))

(defn- internal-default-layout
  "The default template if no `layout` key is specified.
  This lets users know they need to build a `_layouts/default.clj`"
  [{:keys [curr-file]}]
  [:main
   [:div (markup/to-html (:as-edn curr-file))]])

(defn get-layout
  "Checks if a layout for a project exists in the config map
  If it does, return the function value of the layout, otherwise the default template "
  [config layout]
  (let [curr-file-name (-> config :curr-file :name)
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
  "Responsible for rendering org content in layouts."
  ;; Render the whole file.
  ([config]
   (let [org-tree (-> config :curr-file :as-edn)
         yield    (markup/to-html org-tree)] ;; this has lots of nil vals in it.
     yield))

  ([config headline-name]
   (let [org-tree (-> config :curr-file :as-edn)
         headline (org/get-headline org-tree headline-name)]
     (markup/to-html headline)))

  ;; pass in a keyword to retrieve some munged piece of the data
  ([config headline-name piece]
   (let [org-tree         (-> config :curr-file :as-edn)
         headline         (org/get-headline org-tree headline-name)
         headline-content (org/get-headline-content org-tree headline-name)]
     (case piece
       :title      (-> headline :children first  markup/to-html)
       :title-raw  (-> headline :children first :raw)
       :content    (markup/to-html headline-content)
       :logbook    nil ;; TODO
       :properties nil ;; TODO
       headline))))

(defn prepare-layout
  "Pass functions needed for rendering to configs."
  [config]
  {:render   (partial render config)
   :title    (-> config :curr-file :org-title)
   :partials (config :partials)
   :yield    (render config)
   :config   config})

;; FIXME rename this to `apply-layout`
(defn apply-template
  "If a file has a template, render the file with it, or use the default layout"
  [config layout]
  (let [selected-layout (get-layout config layout)]
    (h/html (selected-layout (prepare-layout config)))))
