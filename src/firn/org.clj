(ns firn.org
  "Functions for managing org-related things.
  Most of these functions are for operating on EDN-fied org-file
  Which are created by the rust binary."
  (:require [hiccup.core :as h]
            [clojure.string :as s]))

(defn get-1st-level-headline-by-name
  "Takes org tree and filters down to a headline + it's contents
  Example: `(get-1st-level-headline-by-name 'Meta' org-tree)`
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

