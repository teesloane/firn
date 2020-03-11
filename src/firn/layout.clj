(ns firn.layout
  (:require [hiccup.core :as h]
            [firn.markup :as markup]))




(defn get-templates
  "Reads in an external file of hiccup templates."
  []
  nil)

(defn apply-template
  "Depending on the layout of an org file, renders a template."
  [config]
  nil)


(defn default-template
  [{:keys [curr-file] :as config}]
  (h/html [:html
           [:head
            [:link {:rel "stylesheet" :href "./assets/css/main.css"}]]
           [:body {} (h/html (markup/to-html (:as-edn curr-file)))]]))
