(ns firn.extracter
  (:require [hiccup.core :as h]
            [firn.example :as ex]
            [clojure.string :as s :refer [trim-newline trim]]))


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
       (map (fn [v]
              (when (= (:raw v) "Meta")
                {:title      (:raw v)
                 :properties (:properties v)})))
       (remove nil?)))


(defn get-1st-level-headline-by-name
  "Takes org tree and filters down to a headline + it's contents
  FIXME: Needs to handle cases where nothing is found."
  [name org-tree]
  (->> org-tree
       (map (fn [v]
              (when (= (:type v) "headline") v)))
       (filter (fn [y] ;; get level 1 headings
                 (= (get y :level nil) 1)))
       (filter (fn [y]
                 (= (-> y
                        :children
                        first
                        :children
                        first
                        :value
                        trim) name)))
       (remove nil?)))


;; General funcs for html.
(s/includes? "file:research_org_mode_2020-02-28--20-56.org" "file")



(defn a->html
  "Parses links from the org-tree.
  Checks if a link is an HTTP link or File link."
  [v]
  ()
  (let [file-link? (s/includes? (v :path) "file:")]
    (if file-link?
        (h/html [:a {:href (get v :path "missing")} (get v :desc "missing")])
        (h/html [:a {:href (get v :path "missing")} (get v :desc "missing")]))))

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
      (= "link" t)        (a->html v)
      (= "text"        t) (h/html [:p val])
      :else               "<MISSING HTML VALUE FIXME>")))

(to-html ex/ex5)


;; --- General funcs for preparing the big'un




(defn trxer
  "Takes the AST and parses the info for everything."
  [#_org-file]

  (let [org-tree  (tree-seq map? :children ex/ex #_org-file)
        meta      (get-1st-level-headline-by-name "Meta" org-tree)
        notes     (get-1st-level-headline-by-name "Notes" org-tree)
        resources (get-1st-level-headline-by-name "Resources" org-tree)]

    (-> notes first :children second to-html)))
    ;; {:in-buffer-settings {:data (get-in-buffer-settings org-tree)}
    ;;  :meta               {:data (get-meta org-tree)}}))

(trxer)
