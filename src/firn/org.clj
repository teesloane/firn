(ns firn.org
  "Functions for managing org-related things.
  Most of these functions are for operating on EDN-fied org-file
  Which are created by the rust binary."
  (:require [clojure.string :as s]))

(defn get-headline
  "Fetches a headline from an org-mode tree.
  FIXME: Cannot handle headlines that have links in them."
  [tree name]
  (let [pred (fn [i]
               ;; the node if a headline.
               (and (= "headline" (:type i))
                    ;; the headlines first child (the `title`) has a `some` val of the children equalling the name
                    (some #(= (s/trim (% :value)) name) (-> i :children first :children))))]
                   
    (->> (tree-seq map? :children tree)
         (filter pred)
         (first))))

