(ns firn.markup
  (:require [hiccup.core :as h]
            [clojure.string :as s]
            [firn.example :as ex]))


;; Helpers

(defn flatten-one-level [coll]
  (into [] (mapcat  #(if (sequential? %) % [%]) coll)))

(flatten-one-level [1 [2 3] [4 [5]]])



;;
(defn- parse-file-link
  "FIXME move this to regex.
  AND FIXME: add a base_path into config? (with a partial?) "
  [s]
  (str "/" (-> s (s/split #":") (second) (s/split #"\.") (first))))

;; Renderers

(defn- a->html
  "Parses links from the org-tree.
  Checks if a link is an HTTP link or File link."
  [v]
  (let [link-href  (get v :path "missing!")
        file-link? (s/includes? link-href "file:")
        link-val   (get v :desc "missing!")
        parse-link (if file-link? (parse-file-link link-href) link-href)]
      [:a {:href parse-link} link-val]))

(defn- title-level->html
  " "
  [v]
  (case (v :level)
    1 :h1
    2 :h2 
    3 :h3 
    4 :h4 
    5 :h5 
    :h6))


(defn- src-block->html
  [{:keys [contents language arguments] :as src-block}]
  [:pre contents])




(defn to-html
  "RECURSIVE.
  Parses the org-tree tree-seq into html.
  Don't destructure - it can create uneven maps from possible nil vals on `V`"
  [v]
  (let [type       (v :type)
        children   (v :children)
        value      (v :value)
        val        (if value (s/trim-newline value) value)
        inner-html #(if (empty? children)
                      ""
                      [%  [(flatten-one-level (map to-html children))]])]
   (case type
      "document"     (inner-html :body)
      "headline"     (inner-html :div)
      "title"        (inner-html (title-level->html v))
      "section"      (inner-html :section)
      "paragraph"    (inner-html :div)
      "underline"    (inner-html :i)
      "italic"       (inner-html :em)
      "bold"         (inner-html :strong)
      "list"         (inner-html :ul)
      "list-item"    (inner-html :li)
      "quote-block"  (inner-html :div.quote-block) ;; TODO: fixme
      "source-block" (src-block->html v)
      "link"         (a->html v)
      "code"         [:code val]
      "text"         [[:span val]]
      ;; default value.
      [:span (str "<missing type!>" type " val is " value)])))

(to-html ex/ex)


(into [] (concat))


;; => [:body
;;     ([:section
;;       ([:span "<missing type!>keyword val is Tenses"]
;;        [:span "<missing type!>keyword val is 2020-02-28--08-31"]
;;        [:span "<missing type!>keyword val is research"])]
;;      [:div
;;       ([:h1 ([:span "Meta"])]
;;        [:section ([:span "<missing type!>drawer val is "])])]
;;      [:div ([:h1 ([:span "Resources"])])]
;;      [:div
;;       ([:h1 ([:span "Notes"])]
;;        [:section
;;         ([:div
;;           ([:span "It has taken me learning a "]
;;            [:a
;;             {:href "/research_learning_french_2020-02-24--21-20"}
;;             "new language�"]
;;            [:span
;;             " that I have realized I don't know what\r\nmost of the words surrounding describing tenses are in my native language."])]
;;          [:ul
;;           ([:li
;;             ([:div
;;               ([:span
;;                 "The simple past in english is used to show a completed action that took place"])])])]
;;          [:div
;;           ([:span "at a time in the past. "]
;;            [:i ([:span "Example"])]
;;            [:span ": I "]
;;            [:strong ([:span "lost"])]
;;            [:span " my wallet on Sunday, so I "]
;;            [:strong ([:span "bought"])]
;;            [:span "  a\r\nnew one yesterdray."])]
;;          [:ul
;;           ([:li
;;             ([:div
;;               ([:span
;;                 "The present perfect is used with has/have and a past particible. In "]
;;                [:a {:href "/research_learning_french_2020-02-24--21-20"} "French"]
;;                [:span ",\r\n  this is similarly done using "]
;;                [:code "avoir"]
;;                [:span "."])])])]
;;          [:div.quote-block ([:div ([:span "fail"])])]
;;          [:pre "function sum (x, y) {\r\n    return 1 + y\r\n}\r\n"])])])]




;; (h/html [:div
;;           [:span
;;            [:div "hi"]
;;            [:span "hi"]]])


(defn template
  [org-tree]
  ;; (h/html (first (to-html org-tree))))
  (h/html [:html
           [:head
            [:link {:rel "stylesheet" :href "./assets/main.css"}]]
           [:body {} (h/html (to-html org-tree))]]))
;; => #'firn.markup/template;; => #'firn.markup/template
