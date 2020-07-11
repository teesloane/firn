(ns firn.markup
  "Namespace responsible for converted org-edn into html."
  (:require [clojure.string :as s]
            [firn.util :as u]
            [firn.org :as org]))

(declare to-html)

;; Feature: Table of Contents --------------------------------------------------

(defn make-toc-helper-reduce
  "(ಥ﹏ಥ) Yeah. So. See the docstring for make-toc.
  Basically, this is a bit of a nightmare. This turns a flat list into a tree
  So that we can property create nested table of contents."
  [{:keys [out prev min-level] :as acc} curr]
  (cond
    ;; top level / root headings.
    (or (empty? out) (= min-level (curr :level)))
    (let [with-meta (assoc curr :next-sibling [:out (count out)])
          with-meta (assoc with-meta :next-child [:out (count out) :children])]
      (-> acc
          (update :out conj with-meta)
          (assoc :prev with-meta)))

    ;; if the new items level >= prev item, go to the last item in out
    ;; iterate through children, and try and find `prev`, when you do, collect "path to prev"
    ;; if/when you do update the child list.
    (> (curr :level) (prev :level))
    (let [parent-path (count (get-in acc (prev :next-child)))
          with-meta   (assoc curr :next-sibling (prev :next-child))
          with-meta   (assoc with-meta :next-child (conj (prev :next-child) parent-path :children))]
      (-> acc
          (update-in (prev :next-child) conj with-meta)
          (assoc :prev with-meta)))

    (= (curr :level) (prev :level))
    (let [parent-path (count (get-in acc (prev :next-sibling)))
          with-meta   (assoc curr :next-sibling (prev :next-sibling)) ;; if more, add children, if equal, conj onto children.
          with-meta   (assoc with-meta :next-child (conj (prev :next-sibling) parent-path :children))] ;; if more, add children, if equal, conj onto children.
      (-> acc
          (update-in (prev :next-sibling) conj with-meta)
          (assoc :prev with-meta)))

    (< (curr :level) (prev :level))
    (let [difference   (- (prev :level) (curr :level)) ; if we are on level 5, and the next is level 3...
          diff-to-take (* difference 2)                ; we need to take (5 - 3 ) * 2 = 4 items off the last :next-sibling
          ;; HACK: we can use the prev-elements :next-sibling path and chop N
          ;; elements off the ending based on our heading's leve; which gives us
          ;; the path to conj onto.
          path         (vec (drop-last diff-to-take (prev :next-sibling)))
          parent-path  (count (get-in acc path))
          with-meta    (assoc curr :next-sibling path) ;; if more, add children, if equal, conj onto children.
          with-meta    (assoc with-meta :next-child (conj path parent-path :children))]
      (-> acc
          (update-in path conj with-meta)
          (assoc :prev with-meta)))

    :else
    (do (println "Something has gone wrong. ") acc)))

(defn toc->html
  [toc kind]
  (->> toc
       (map (fn [x]
              (if (empty? (x :children))
                [:li
                 [:a {:href (x :anchor)} (x :text)]]
                [:li
                 [:a {:href (x :anchor)} (x :text)]
                 [kind (toc->html (x :children) kind)]])))))

(defn make-toc
  "toc: a flattened list of headlines with a :level value of 1-> N:
  [{:level 1, :text 'Process', :anchor '#process'}  {:level 2, :text 'Relevance', :anchor '#relevance'}]

  We conditonally thread the heading, passing in configured values, such as
  where to start the table of contents (at a specific headline?)
  or unto what depth we want the headings to render."
  ([toc]
   (make-toc toc {}))
  ([toc {:keys [headline depth list-type exclusive?]
         :or   {depth nil list-type :ol}
         :as   opts}]
   (let [s-h         (u/find-first #(= (% :text) headline) toc)     ; if user specified a heading to start at, go find it.
         toc         (cond->> toc                                   ; apply some filtering to the toc, if params are passed in.
                       depth      (filter #(<= (% :level) depth))   ; if depth; keep everything under that depth.
                       headline   (drop-while #(not= s-h %))        ; drop everything up till the selected heading we want.
                       headline   (u/take-while-after-first         ; remove everything that's not a child of the selected heading.
                                   #(> (% :level) (s-h :level)))
                       exclusive? (drop 1))                         ; don't include selected heading; just it's children.)

         min-level   (if (seq toc) (:level (apply min-key :level toc)) 1) ; get the min level for calibrating the reduce.
         toc-cleaned (->> toc
                          (map #(assoc % :children []))  ; create a "children" key on every item.)
                          (reduce make-toc-helper-reduce {:out [] :prev nil :min-level min-level})
                          :out)]

     (if (empty? toc-cleaned) nil
         (into [list-type] (toc->html toc-cleaned list-type))))))

;; General Renderers -----------------------------------------------------------

(defn date->html
  [v]
  (let [{:keys [year month day hour minute]} (v :start)]
    [:span.firn-timestamp (str year "/" month "/" day
                               (when (and hour minute) hour ":" minute))]))

(defn props->html
  "Renders heading properties to html."
  [{:keys [properties] :as v}]
  [:div.firn-properties
   (for [[k v] properties]
     [:div
      [:span.firn-property-key k ": "]
      [:span.firn-property-value v]])])

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
      (str "/" (nth res 2) anchor-link)
      (str "/" (nth res 2)))))

(defn link->html
  "Parses links from the org-tree.
  Checks if a link is an HTTP link or File link."
  [v opts]
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
      [:a.firn-internal {:href (str #p (opts :site-url) (internal-link-handler link-href))} link-val]

      (re-matches http-link-regex link-href)
      [:a.firn-external {:href link-href :target "_blank"} link-val]

      ;; Otherwise, assume it's an internal anchor link.
      :else
      [:a {:href (u/clean-anchor link-href)} link-val])))

(defn- title->html
  "Constructs a headline title - with possible additional values
  (keywords, priorities, timestamps -> can all be found in a headline.)
  That aren't found in the `children` values and so need special parsing."
  [v opts]
  (let [level            (v :level)
        children         (v :children)
        keywrd           (v :keyword)
        tags             (v :tags)
        priority         (v :priority)
        properties       (v :properties)
        parent           {:type "headline" :level level :children [v]} ; reconstruct the parent so we can pull out the content.
        heading-priority (u/str->keywrd "span.firn-headline-priority.firn-headline-priority__" priority)
        heading-keyword  (u/str->keywrd "span.firn-headline-keyword.firn-headline-keyword__" keywrd)
        heading-tags     [:span.firn-tags (for [t tags] [:span [:a.firn-tag {:href (str "/tags#" t)} t]])]
        heading-anchor   (org/make-headline-anchor parent)
        heading-id+class #(u/str->keywrd "h" % heading-anchor ".firn-headline.firn-headline-" %)
        h-level          (case level
                           1 (heading-id+class 1)
                           2 (heading-id+class 2)
                           3 (heading-id+class 3)
                           4 (heading-id+class 4)
                           5 (heading-id+class 5)
                           (heading-id+class 6))
        make-child       #(into [%] (map to-html children))
        render-headline  [h-level
                          (when keywrd [heading-keyword (str keywrd " ")])
                          (when priority [heading-priority (str priority " ")])
                          (make-child :span.firn-headline-text)
                          (when tags heading-tags)]]

    (if (and properties (opts :firn-properties?))
      [:div render-headline (props->html v)]
      render-headline)))

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
  ([v] (to-html v {}))
  ([v opts]
   (let [type           (get v :type)
         children       (get v :children)
         value          (get v :value)
         value          (if value (s/trim-newline value) value)
         ordered        (get v :ordered) ;; for lists
         headline-level (get v :level)
         headline-el    (u/str->keywrd "div.firn-headline-section.firn-headline-section-" headline-level)
         make-child     #(into [%] (map (fn [c] (to-html c opts)) children))]
     (case type
       "document"      (make-child :div)
       "headline"      (make-child headline-el)
       "title"         (title->html v opts)
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
       "link"          (link->html v opts)
       "fn-ref"        (footnote-ref v)
       "fn-def"        (footnote-def v)
       "code"          [:code value]
       "verbatim"      [:code value]
       "rule"          [:hr]
       "cookie"        [:span.firn-cookie value]
       "text"          [:span value]
       "timestamp"     (date->html v)
       "keyword"       "" ;; Don't parse
       "comment-block" "" ;; Don't parse
       "drawer"        "" ;; Don't parse
       ;; default value. NOTE: Might be ideal to have a "if dev-mode -> show unparsed block"
       ""))))
