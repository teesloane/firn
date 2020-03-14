(ns firn.org
  "Functions for managing org-related things.
  Most of these functions are for operating on EDN-fied org-file
  Which are created by the rust binary."
  )

(defn get-headline
  "Fetches a headline from an org-mode tree.
  FIXME: using `:raw` is brittle. It does not account for headlines with dynamic content."
  [tree name]
  (let [pred #(and (= "headline" (:type %))
                   (= name (-> % :children first :raw)))]
    (->> (tree-seq map? :children tree)
         (filter pred)
         (first))))

