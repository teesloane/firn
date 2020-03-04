(ns firn.orgformer
  (:require [hiccup.core :as h]
            [firn.example :as ex]
            [firn.markup :as m]
            [clojure.string :as s]
            [cheshire.core :as json]))



;; -- Prepare the org tree

(defn org-edn->org-tree
  "Takes a read org-file in edn format and preps it to be worked with as a tree."
  [{:keys [file-edn] :as config}]
  (let [file-keywords           (get-in file-edn [:children 0 :children])
        file-edn-props-stripped (assoc-in file-edn [:children 0 :children] [])
        org-tree                (->> file-edn-props-stripped (tree-seq map? :children) (first))]
    (assoc config
           :org-tree org-tree
           :file-keywords file-keywords)))


(defn org-tree->html-out
  "Takes the org tree and renders it to html
  TODO: eventually render html conditional to file type."
  [config]
  (let [out-html (-> config :org-tree m/template)]
    (assoc config :out-html out-html)))


(defn get-1st-level-headline-by-name
  "Takes org tree and filters down to a headline + it's contents
  FIXME: Needs to handle cases where nothing is found."
  [name org-tree]
  (->> org-tree
       (map (fn [v]
              (when (= (:type v) "headline") v)))
       ;; get level 1 headings
       (filter #(= (get % :level nil) 1))
       ;; get at the (non-raw) heading if it matches `name`
       (filter #(= (-> % :children first :children first :value s/trim) name))
       (remove nil?)
       (first)))



(defn trxer
  "Takes the AST and parses the info for everything."
  [{:keys [file-name file-edn] :as config}]
  (-> config
      (org-edn->org-tree)
      (org-tree->html-out))

  #_(let [org-tree  (org-edn->org-tree config)]
        ;; meta      (get-1st-level-headline-by-name "Meta" org-tree)
        ;; notes     (get-1st-level-headline-by-name "Notes" org-tree)
        ;; resources (get-1st-level-headline-by-name "Resources" org-tree)
        out-html
      (assoc config :out-html out-html))
    #_(-> notes to-html flatten s/join))

