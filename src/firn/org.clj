(ns firn.org
  "Functions for managing org-related things.
  Most of these functions are for operating on EDN-fied org-file
  Which are created by the rust binary."
  (:require [clojure.string :as s]))
           

(defn- get-headline-helper
  "Sanitizes a heading of links and just returns text.
  Necessary because org leafs of :type `link` have a `:desc` and not a `:value`

  UGH. this is a mess of a function, and should be refactored. Basically:
  - Loop through the children that represent a title.
  - get the values out and trim them incase there is whitespace
  (between say, a cookie or a priority, or empty text)
  - Then, filter out all empty strings
  - Then join them together.

  All in all, this is supposed to remove the dynamic elements of a heading so user's don't have to
  search headlines that have percentages, or priorities, etc."
  [headline]
  (let [title-children  (-> headline :children first :children)
        get-trimmed-val #(let [trimmed-val (s/trim (get %1 %2))]
                           (if (empty? trimmed-val) "" trimmed-val))]
    (s/join " "
            (filter not-empty
                    (for [child title-children]
                      (s/trim
                       (case (:type child)
                         "text" (get-trimmed-val child :value)
                         "link" (get-trimmed-val child :desc)
                         "")))))))


(defn get-headline
  "Fetches a headline from an org-mode tree."
  [tree name]
  (->> (tree-seq map? :children tree)
       (filter #(and (= "headline" (:type %))
                     (= name (get-headline-helper %))))
       (first)))


(defn get-headline-content
  "Same as get-headline, but removes the first child :title)."
  [tree name]
  (let [headline (get-headline tree name)]
    (update headline :children (fn [d] (filter #(not= (:type %) "title") d)))))
