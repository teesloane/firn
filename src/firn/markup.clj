(ns firn.markup
  (:require [hiccup.core :as h]
            [clojure.string :as s]))


;; Helpers
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
      (h/html [:a {:href parse-link} link-val])))

(defn- heading->html
  "Parses headings
  Needs to be able to parse headings that don't have simple text:
  <* TODO [2020-02-25 Tue] Setup <2020-03-03 Tue> emacs spell check in org mode>
  For example, in which the heading has interleaved non-text values in the heading.
  "
  [{:keys [level raw children keyword] :as v}]
  (let [text-content (->> children
                          (filter #(= (% :type) "text"))
                          (mapcat #(str (% :value))))]
    (case level
      1 (h/html [:h1 text-content])
      2 (h/html [:h2 text-content])
      3 (h/html [:h3 text-content])
      4 (h/html [:h4 text-content])
      5 (h/html [:h5 text-content])
      (h/html [:h6 text-content]))))


(defn- src-block->html
  [{:keys [contents language arguments] :as src-block}]
  (h/html [:pre contents]))


(defn to-html
  "Should expect the first value to be of type `:section`
  Is generally responsible for parsing org content (no headlines, etc)
  Should not be encountering `:type` of `:heading` etc.
  Not destructuring because it could create uneven maps.
  https://stackoverflow.com/a/47040814 "
  [v]
  (let [type       (v :type)
        children   (v :children)
        value      (v :value)
        val        (if value (s/trim-newline value) value) ;; tri
        ;; TODO - if children is empty return nothing...
        inner-html #(h/html [% (map to-html children)])
        inner-html #(if (empty? children)
                     ""
                     (h/html [% (map to-html children)]))]
    (case type
      "document"     (map to-html children)
      "headline"     (map to-html children)
      "title"        (heading->html v)
      "section"      (inner-html :section)
      "paragraph"    (inner-html :div)
      "underline"    (inner-html :i)
      "bold"         (inner-html :strong)
      "list"         (inner-html :ul)
      "list-item"    (inner-html :li)
      "quote-block"  (inner-html :div.quote-block) ;; TODO: fixme
      "source-block" (src-block->html v)
      "link"         (a->html v)
      "code"         (h/html [:code val])
      "text"         (h/html [:span val])
      ;; default value.
      (h/html [:span "missing type:" type " val is " value]))))
