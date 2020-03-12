(ns firn.layout
  (:require [hiccup.core :as h]
            [hiccup.def :refer :all]
            [firn.markup :as markup]
            [firn.config :as config]))

(defn get-templates
  "Reads in an external file of hiccup templates."
  []
  nil)


(defn templ-wrapper
  "Default shared wrapper around ALL files."
  [body]
  [:html
   [:head
    [:link {:rel "stylesheet" :href "https://unpkg.com/tachyons@4.10.0/css/tachyons.min.css"}]
    [:link {:rel "stylesheet" :href "./assets/css/main.css"}]]
   [:body body]])


(defn templ-project
  "Renders a templates as if a project."
  [config]
  (templ-wrapper
   [:main.debug.m7
    [:aside.fl.w-25.pa2
     [:div.pa2.pr4 "Sidebar stuff"]]
    [:div.fl.w-10.pa2]
    [:article.fl.w-60.pa2
     [:div.pa2 "Content"]]]))

(defn default-template
  "The default template if no `layout` key is specified."
  [{:keys [curr-file] :as config}]
  (templ-wrapper
   [:main (markup/to-html (:as-edn curr-file))]))

(defn apply-template
  "Depending on the layout of an org file, renders a template."
  [config template]
  (h/html
   (case template
     "project" (templ-project config)
     (do
       (println "Template: <" template "> not found for file:" (config/get-curr-file-name config))
       (default-template config)))))
