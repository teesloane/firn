(ns firn.markup
  "Namespace responsible for converted org-edn into html."
  (:require [clojure.string :as s]
            [firn.util :as u]
            [firn.org :as org]))

(declare to-html)

;; Renderers
;;
(defn make-toc-helper
  ""
  [input]
  (let [min-level   (:level (apply min-key :level input))             ; get the minimum level present in the table of contents.
        floor-input (map #(update % :level - (dec min-level))  input) ; if the lowest level is "4", iterate and minus everything to 1 (so as not to start with nested ul's)
        split-up    (partition-by #(identity (% :level)) floor-input) ; input looks like ({:anchor "#le-présent-progressif-(the-present-tense)", :level 3, :raw "Le présent progressif (The /very/ present tense)"} {:anchor "#le-passé-récent-(the-tense)", :level 3, :raw "Le passé récent (The /just happened/ tense)"}))
        make-html   #(vector :li [:a {:href (% :anchor)} (% :raw)])   ; map fn for converting the input toc into html.
        toc         (into [:ul]                                       ; here we loop through the split up items,
                          (for [x    split-up
                                :let [level (:level (first x))      ; get the current level
                                      li-d  (map make-html x)]]     ; convert the toc map into html.
                            ;; now, we nest nest nest nest nest nest.
                            (case level
                              1 li-d
                              2 [:li (into [:ul] li-d)]
                              3 [:li [:ul [:li (into [:ul] li-d)]]]
                              4 [:li [:ul [:li [:ul [:li (into [:ul] li-d)]]]]]
                              5 [:li [:ul [:li [:ul [:li [:ul [:li (into [:ul] li-d)]]]]]]]
                              [:li [:ul [:li [:ul [:li [:ul [:li [:ul [:li (into [:ul] li-d)]]]]]]]]])))]
    toc))

(defn make-toc
  "toc: a flattened list of headlines with a :level value of 1-> N:
  [{:level 1, :raw 'Process', :anchor '#process'}  {:level 2, :raw 'Relevance', :anchor '#relevance'}]

  We conditonally thread the heading, passing in configured values, such as
  where to start the table of contents (at a specific headline?)
  or unto what depth we want the headings to render."
  ([toc]
   (make-toc toc {}))
  ([toc {:keys [headline depth]
         :or   {depth nil}}]

   (let [starting-heading   (u/find-first #(= (% :raw) headline) toc)
         h>starting-heading #(> (% :level) (starting-heading :level))
         h<depth            #(< (% :level) depth)
         just-depth         (and depth (nil? headline))
         toc-cleaned        (cond->> toc
                              ;; if we pass in a heading...
                              headline             (drop-while #(not= starting-heading %)) ; drop all headings till matching the starting-heading
                              headline             (rest)                                  ; Don't count the first heading
                              (and headline depth) (take-while #(and (h>starting-heading %) (h<depth %)))
                              just-depth           (filter #(< (% :level) depth)))]
     (if (empty? toc-cleaned) nil
         (make-toc-helper toc-cleaned)))))

(defn date->html
  [v]
  (let [{:keys [year month day hour minute]} (v :start)]
    [:span (str year "/" month "/" day
                (when (and hour minute) hour ":" minute))]))

(defn- src-block->html
  "Formats a org-mode src block.
  NOTE: Has additional :keys `language` and `arguments`
  that could be used for syntax highlighting"
  [{:keys [contents _language _arguments] :as _src-block}]
  [:pre
   [:code contents]])

(defn img-link->figure
  "Renders an image with a figure if the link has a :desc, otherwise, :img"
  [{:keys [desc path]}]
  ;; NOTE: I would like to use :figure/figcaption here but we can't
  ;; https://stackoverflow.com/a/5163443
  (if desc
    [:span.firn-img-with-caption
     [:img {:src path}]
     [:span.firn-img-caption desc]]
    [:img {:src path}]))

(defn internal-link-handler
  "Takes an org link and converts it into an html path."
  [org-link]
  (let [regex       #"(file:)(.*)\.(org)(\:\:\*.+)?"
        res         (re-matches regex org-link)
        anchor-link (last res)
        anchor-link (when anchor-link (-> res last u/clean-anchor))]
    (if anchor-link
      (str "./" (nth res 2) anchor-link)
      (str "./" (nth res 2)))))

(defn link->html
  "Parses links from the org-tree.
  Checks if a link is an HTTP link or File link."
  [v]
  (let [link-val        (get v :desc)
        link-href       (get v :path "Missing HREF attribute.")
        ;; img regexs / ctor fns.
        img-file-regex  #"(file:)(.*)\.(jpg|JPG|gif|GIF|png)"
        img-http-regex  #"(http:\/\/|https:\/\/)(.*)\.(jpg|JPG|gif|GIF|png)"
        img-rel-regex   #"(\.(.*))\.(jpg|JPG|gif|GIF|png)"
        img-make-url    #(->> (re-matches img-file-regex link-href)
                              (take-last 2)
                              (s/join "."))
        ;; file regexs / ctor fns
        org-file-regex  #"(file:)(.*)\.(org)(\:\:\*.+)?"
        http-link-regex #"https?:\/\/(?![^\" ]*(?:jpg|png|gif))[^\" ]+"]

    (cond
      ;; Images ---
      ;; img file or attach: `file:`
      (re-matches img-file-regex link-href)
      (img-link->figure {:desc link-val :path (img-make-url)})

      ;; relative: `../../foo.jpg`
      (re-matches img-rel-regex link-href)
      (img-link->figure {:desc link-val :path link-href})

      ;; a normal http image.
      (re-matches img-http-regex link-href)
      (img-link->figure {:desc link-val :path link-href})

      ;; org files
      (re-matches org-file-regex link-href)
      [:a.firn_internal {:href (internal-link-handler link-href)} link-val]

      (re-matches http-link-regex link-href)
      [:a.firn_external {:href link-href :target "_blank"} link-val]

      :else
      [:a {:href link-href}])))

(defn- title->html
  "Constructs a headline title - with possible additional values
  (keywords, priorities, timestamps -> can all be found in a headline.)
  That aren't found in the `children` values and so need special parsing."
  [v]
  (let [level            (v :level)
        typ              (v :type)
        children         (v :children)
        keywrd           (v :keyword)
        priority         (v :priority)
        value            (v :value)
        parent           {:type "headline" :level level :children [v]} ; reconstruct the parent so we can pull out the content.
        heading-priority (u/str->keywrd "span.firn-headline-priority.firn-headline-priority__" priority)
        heading-keyword  (u/str->keywrd "span.firn-headline-keyword.firn-headline-keyword__" keywrd)
        heading-anchor   (org/make-headline-anchor parent) #_(-> parent org/get-headline-helper u/clean-anchor)
        heading-id+class #(u/str->keywrd "h" % heading-anchor ".firn-headline.firn-headline-" %)
        h-level          (case level
                           1 (heading-id+class 1)
                           2 (heading-id+class 2)
                           3 (heading-id+class 3)
                           4 (heading-id+class 4)
                           5 (heading-id+class 5)
                           (heading-id+class 6))
        make-child       #(into [%] (map title->html children))]
    (case typ
      "headline"  (make-child :div)
      "title"     [h-level
                   (when keywrd [heading-keyword (str keywrd " ")])
                   (when priority [heading-priority (str priority " ")])
                   (make-child :span)]
      "text"      [:span value]
      "cookie"    [:span.firn-headline-cookie value]
      "timestamp" [:span.firn-headline-timestamp (date->html v)]
      "code"      [:code value]
      "verbatim"  [:code value]
      "link"      (link->html v)
      "underline" (make-child :i)
      "italic"    (make-child :em)
      "bold"      (make-child :strong)
      "")))

(defn- footnote-ref
  [v]
  [:a.firn-footnote-ref
   {:id   (str "fn-" (v :label))
    :href (str "#" (v :label))}
   [:sup (v :label)]])

(defn- footnote-def
  [v]
  (let [make-child     #(into [%] (map to-html (v :children)))]
    [:span.firn-footnote-def
     [:span {:id (v :label)
             :style "padding-right: 8px"} (v :label)]
     (make-child :span)
     [:a {:href (str "#fn-" (v :label))
          :style "padding-left: 4px"} "↩"]]))

(defn to-html
  "Recursively Parses the org-edn into hiccup.
  Some values don't get parsed (drawers) - yet. They return empty strings.
  Don't destructure! - with recursion, it can create uneven maps from possible nil vals on `v`"
  [v]
  (let [type           (get v :type)
        children       (get v :children)
        value          (get v :value)
        ordered        (get v :ordered)                               ;; for lists
        val            (if value (s/trim-newline value) value)
        headline-level (get v :level)
        headline-el    (u/str->keywrd "div.firn_headline-section.firn_headline-section-" headline-level)
        make-child     #(into [%] (map to-html children))]
    (case type
      "document"      (make-child :div)
      "headline"      (make-child headline-el)
      "title"         (title->html v)
      "section"       (make-child :section)
      "paragraph"     (make-child :p)
      "underline"     (make-child :u)
      "italic"        (make-child :em)
      "bold"          (make-child :strong)
      "list"          (make-child (if ordered :ol :ul))
      "list-item"     (make-child :li)
      "quote-block"   (make-child :blockquote)
      "table"         (make-child :table)
      "table-row"     (make-child :tr)
      "table-cell"    (make-child :td)
      "source-block"  (src-block->html v)
      "link"          (link->html v)
      "fn-ref"        (footnote-ref v)
      "fn-def"        (footnote-def v)
      "code"          [:code val]
      "verbatim"      [:code val]
      "rule"          [:hr]
      "cookie"        [:span.cookie val]
      "text"          [:span val]
      "timestamp"     (date->html v)
      "keyword"       "" ;; Don't parse
      "comment-block" "" ;; Don't parse
      "drawer"        "" ;; Don't parse
      ;; default value. NOTE: Might be ideal to have a "if dev-mode -> show unparsed block"
      "")))
