(ns firn.layout
  (:require [hiccup.core :as h]
            [hiccup.def :refer :all]
            [firn.markup :as markup]
            [firn.util :as u]
            [firn.config :as config]
            [me.raynes.fs :as fs]))

(defn get-templates
  "Reads in a _layouts dir of clj/hiccup templates."
  [config]
  (let [layout-files  (fs/find-files (config :layouts-dir) #"^.*\.(clj)$")
        layouts-map   (into {} (map #(hash-map (u/io-file->keyword %) %) layout-files))]
    (assoc config :layouts layouts-map)))

(defn templ-wrapper
  "Default shared wrapper around ALL files."
  [body]
  [:html
   [:head
    [:link {:rel "stylesheet" :href "https://unpkg.com/tachyons@4.10.0/css/tachyons.min.css"}]
    [:link {:rel "stylesheet" :href "./assets/css/main.css"}]]
   [:body body]])


;; TODO - this would be replaced by "_layouts/default.clj"
(defn default-template
  "The default template if no `layout` key is specified."
  [{:keys [curr-file] :as config}]
  (templ-wrapper
   [:main (markup/to-html (:as-edn curr-file))]))

(defn apply-template
  "Depending on the layout of an org file, renders a template."
  [config layout]
  (prn "layout is" layout (type layout))
  (if layout
    (let [layout-file-path (-> config :layouts layout .getPath)
          loaded-layout    (-> layout-file-path (slurp) (read-string) (eval))]
      (h/html (loaded-layout config templ-wrapper)))
    (h/html (default-template config))))
