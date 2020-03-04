(ns firn.orgformer
  (:require [hiccup.core :as h]
            [firn.example :as ex]
            [firn.markup :as m]
            [clojure.string :as s]
            [cheshire.core :as json]))



(defn dataify-file
  "Converts an org file into a bunch of data."
  [{:keys [file-name file-json] :as config}]
  (let [file-edn             (-> file-json (:out) (json/parse-string true))
        file-keywords        (get-in file-edn [:children 0 :children])
        file-edn-no-keywords (assoc-in file-edn [:children 0 :children] [])
        org-tree             (->> file-edn-no-keywords (tree-seq map? :children) (first))
        org->html            (m/template org-tree)]
    (conj
     config
     {:file-edn      file-edn
      :file-keywords file-keywords
      :org-tree      org-tree
      :out-html      org->html})))

    


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
       (first))



  #_(let [org-tree  (org-edn->org-tree config)]
        ;; meta      (get-1st-level-headline-by-name "Meta" org-tree)
        ;; notes     (get-1st-level-headline-by-name "Notes" org-tree)
        ;; resources (get-1st-level-headline-by-name "Resources" org-tree)
        out-html
      (assoc config :out-html out-html))
    #_(-> notes to-html flatten s/join))

