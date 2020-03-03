(ns firn.extracter
  (:require [hiccup.core :as h]
            [firn.example :as ex]
            [clojure.string :refer [trim-newline]]))



(defn- get-headline-by-name
  [v]
  (when (= (:raw v) "Meta")
    v))


;; -- Get pieces out of the Org Map

(defn get-in-buffer-settings
  "Takes org-tree and gets all in-buffer-settings from it."
  [org-tree]
  (->> org-tree
       (map #(when (= (:type %) "keyword")
               {(:key % ) (:value %)}))
       (remove nil?)))


(defn get-meta
  [org-tree]
  (->> org-tree
       (map (fn [v] v
              (when (= (:raw v) "Meta")
                {:title      (:raw v)
                 :properties (:properties v)})))
       (remove nil?)))


(defn to-html
  "Should expect the first value to be of type `:section`
  Is generally responsible for parsing org content (no headlines, etc)
  Should not be encountering `:type` of `:heading` etc."
  [v]
  (let [t          (v :type) ;; possible destructuring?
        c          (v :children)
        val        (v :value)
        val        (if val (trim-newline val) val)
        inner-html #(h/html [% (map to-html c)])]
    (cond
      (= "document"    t) (map to-html (v :children))
      (= "section"     t) (inner-html :section)
      (= "paragraph"   t) (inner-html :div)
      (= "underline"   t) (inner-html :i)
      (= "bold"        t) (inner-html :strong)
      (= "list"        t) (inner-html :ul)
      (= "list-item"   t) (inner-html :li)
      (= "quote-block" t) (inner-html :div) ;; TODO: fixme
      (= "text"        t) (h/html [:p val])
      :else             "<MISSING HTML VALUE FIXME>")))

(to-html ex/ex5)






(->> ex/ex
     (tree-seq map? :children)
     (map to-html))

  






(defn trxer
  "Takes the AST and parses the info for everything."
  [org-file]

  (let [org-tree (tree-seq map? :children org-file)]
    {:in-buffer-settings {:data (get-in-buffer-settings org-tree)}
     :meta               {:data (get-meta org-tree)}}))


