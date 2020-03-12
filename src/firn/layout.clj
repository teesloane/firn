(ns firn.layout
  (:require [hiccup.core :as h]
            [firn.markup :as markup]
            [firn.config :as config]))

(defn get-templates
  "Reads in an external file of hiccup templates."
  []
  nil)

(defn templ-project
  "Renders a templates as if a project."
  [config]
  (h/html [:body "Hup! Seyoot!"]))

(defn default-template
  [{:keys [curr-file] :as config}]
  (h/html [:html
           [:head
            [:link {:rel "stylesheet" :href "./assets/css/main.css"}]]
           [:body {} (h/html (markup/to-html (:as-edn curr-file)))]]))

(defn apply-template
  "Depending on the layout of an org file, renders a template."
  [config template]
  (case template
    "project" (templ-project config)
    (do
      (println "Template: <" template "> not found for file:" (config/get-curr-file-name config))
      (default-template config))))
