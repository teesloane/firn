(ns firn.markup
  (:require [hiccup.core :as h]
            [clojure.string :as s]))



;; Helpers


(defn- parse-file-link
  "FIXME move this to regex.
  AND FIXME: add a base_path into config? (with a partial?) "
  [s]
  (str "./" (-> s (s/split #":") (second) (s/split #"\.") (first))))

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
        inner-html (fn [tag]
                     (if (empty? children)
                       ""
                       (into [tag] (map to-html children))))] ;; << ???
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
      "text"         [:span val]
      ;; default value.
      [:span (str "<missing type!>" type " val is " value)])))


(defn template
  [org-tree]
  (h/html [:html
           [:head
            [:link {:rel "stylesheet" :href "./assets/styles/main.css"}]]
           [:body {} (h/html (to-html org-tree))]]))
