(ns firn.markup
  "Namespace responsible for converted org-edn into html."
  (:require [clojure.string :as s]
            [firn.util :as u]
            [firn.org :as org]))

(declare to-html)
(declare internal-link-handler)

;; Renderers -------------------------------------------------------------------

;; R: Site-map et al. --

(defn render-site-map
  "Converts the site map data structure into html. Takes options for sorting
  This is somewhat complex and has many nested functions for performing sorting..
  This is complex/featureful because a user can:
  - a) sort their map by date and `firn-order`
  - b) a user can choose to start their site-map at a specific 'node'
  - c) we have to handle for when the user's files don't have all the necessary metadata."
  ([sm]
   (render-site-map sm {}))
  ([sm opts]
   (letfn [(sort-by-key ;; used in sort-site-map
             ([smn prop] (sort-by-key smn prop false))
             ([smn prop flip-keys?]
              (fn [key1 key2]
                (let [k1 (if flip-keys? key2 key1)
                      k2 (if flip-keys? key1 key2)]
                  (compare
                   ;; we have to compare on values, and because some are duplicate (nil) we have to use compare a bit differently.
                   ;; https://clojuredocs.org/clojure.core/sorted-map-by#example-542692d5c026201cdc327094
                   [(get-in smn [k1 prop] ) k1]
                   [(get-in smn [k2 prop] ) k2])))))

           ;; Sometimes a user might want to render the site-map at a lower
           ;; level than from the top.
           (starting-point [sm]
             (if (opts :start-at)
               (get-in sm (u/interpose+tail (opts :start-at) :children))
               sm))

           ;; NOTE: make sorting push nils to the end (for cases where say,
           ;; firn-order is nil.) when sorting a sitemap node, the key we sort
           ;; by might not exist. when collections with nil are sorted, nil ends
           ;; up at the beginning of this list. Using Juxt, we can push them to
           ;; the end.
           ;;
           ;; The use of `map` and `into` in the thread macro are around just
           ;; for keeping the shape of the map since we are going from
           ;; map -> list -> map, and the keys of the map MIGHT not exist as files,
           ;; (due to how the site-map is built with `firn-under`
           ;; and thus they also might not have the sorting key. ＼（＾ ＾）／
           (sort-by-key-nil-at-end [smn k reverse]
             (let [sort-order (fn [a b] (if reverse (compare a b) (compare b a)))]
               (->> smn
                    (map (fn [[k v]] (assoc v :__keyname k))) ; keep the key
                    (sort-by (juxt #(nil? (% k)) k) sort-order)
                    (map #(hash-map (% :__keyname) %))
                    (into {}))))

           (sort-site-map [site-map-node] ;; site-map-node is a whole site-map or any :children sub maps.
             (case (opts :sort-by)
               :alphabetical (into (sorted-map) site-map-node)
               :newest       (into (sorted-map-by (sort-by-key site-map-node :date-created-ts true)) site-map-node)
               :oldest       (into (sorted-map-by (sort-by-key site-map-node :date-created-ts false)) site-map-node)
               :firn-order   (sort-by-key-nil-at-end site-map-node :firn-order true)
               site-map-node))

           (make-child [[k v]]
             (let [children (v :children)]
               (if-not children
                 [:li (if (v :path)
                        [:a.firn-sitemap-item--link {:href (v :path)} k]
                        [:div.firn-sitemap-item--no-link k])]
                 ;; if children
                 [:li.firn-sitemap-item--child
                  (if (v :path)
                    [:a.firn-sitemap-item--link {:href (v :path)} k]
                    [:div.firn-sitemap-item--no-link k])
                  [:ul.firn-sitemap-item--parent
                   (map make-child (sort-site-map children))]])))]
     (let [starting-site-map (-> sm starting-point sort-site-map)]
       [:ul.firn-sitemap.firn-sitemap-item--parent (map make-child starting-site-map)]))))

(defn render-breadcrumbs
  "Iterates firn-under, fetching the link to each item, constructuring a breadcrumb
  in: firn-under: [Research Language English]
  out             [{:name Research :path /Research} {:name Language :path /Language}]
  :out-data       [:div [:a {:href path} name] ' > ' [:a {:href path} name]]]] etc."
  [firn-under sitemap opts]
  (loop [firn-under   firn-under
         sitemap-node sitemap
         out          []]
    (let [head    (first firn-under)
          tail    (rest firn-under)
          from-sm (get sitemap-node head)
          new-out (conj out {:title head :path (get from-sm :path)})]
      (if (nil? head)
        [:div.firn-breadcrumbs
         ;; construct html
         (->> out
            (map #(vec (if (% :path)
                         [:a {:href (% :path)} (% :title)]
                         [:span (% :title)])))
            (interpose [:span (or (opts :separator) " > ")]))]
        ;; --
        (recur tail (get-in sitemap [head :children]) new-out)))))

(defn render-adjacent-file
  "Renders html (or returns data) of the previous and next link in the sitemap.
  Expects that your files are using `firn-order` or `:date-created-ts` in order to
  properly determine what is `next` and what is `previous`."
  [{:keys [sitemap firn-under firn-order date-created-ts as-data? order-by prev-text next-text]
    :or   {order-by :firn-order}}]
  (let [site-map-node (if (nil? firn-under) sitemap
                          (get-in sitemap (u/interpose+tail firn-under :children)))
        sort-mech     {:date       {:key :date-created-ts :file-val date-created-ts}
                       :firn-order {:key :firn-order :file-val firn-order}}
        sort-key      (-> sort-mech order-by :key)
        org-file-val  (-> sort-mech order-by :file-val)
        ordered-smn   (->> site-map-node vals (sort-by sort-key))
        prev-text     (or prev-text "Previous: ")
        next-text     (or next-text "Next: ")
        out           (atom {:next nil :previous nil})]
    (loop [lst  ordered-smn
           prev nil]
      (when (seq lst)
        (let [head (first lst)]
          ;; when firn-order equals the item we are iterative over.
          (if (= (sort-key head) org-file-val)
            (reset! out {:next (second lst) :previous prev})
            (recur (rest lst) head)))))
    (if as-data? @out
        (let [{:keys [previous next]} @out]
          [:div.firn-file-navigation
           (when previous
             [:span.firn-file-nav-prev
              prev-text [:a {:href (previous :path)} (previous :title)]])
           " "
           (when next
             [:span.firn-file-nav-next
              next-text [:a {:href (next :path)} (next :title)]])]))))

;; R: Backlinks -------

(defn render-backlinks
  [{:keys [site-links file site-url]}]
  (let [org-path-match-file-url? #(let [x (internal-link-handler (% :path) site-url)]
                                    (= x (file :path-url)))
        to-html                  (fn [x] [:li.firn-backlink [:a {:href (x :from-url)} (x :from-file)]])
        backlinks                (->> site-links (filter org-path-match-file-url?))
        backlinks-unique         (map first (vals (group-by :from-url backlinks)))]
    (if (seq backlinks-unique)
      (into [:ul.firn-backlinks] (map to-html backlinks-unique))
      nil)))

;; R: Tags (firn / org)

(defn render-firn-tags
  "Renders a list of tags and their respective files
  The tag list sections themsleves renders alphabetically,

  The per-tag-list can be sorted by user input:

    (render :firn-tags {:sort-by :alphabetical})
    (render :firn-tags {:sort-by :newest})
    (render :firn-tags {:sort-by :oldest})

  Provided files have a #+DATE_CREATED front matter.
  "
  ([firn-tags]
   (render-firn-tags firn-tags {}))
  ([firn-tags opts]
   (let [x-fn      u/sort-map-of-lists-of-maps
         firn-tags (case (opts :sort-by)
                     :alphabetical (x-fn {:coll firn-tags :sort-key :from-file :sort-by :alphabetical})
                     :newest       (x-fn {:coll firn-tags :sort-key :date-created-ts :sort-by :newest})
                     :oldest       (x-fn {:coll firn-tags :sort-key :date-created-ts :sort-by :oldest})
                     firn-tags)]

     [:div.firn-file-tags
      (for [[k lst] firn-tags]
        [:div.firn-file-tags-container
         [:div.firn-file-tag-name k]
         [:ul.firn-file-tag-list
          (for [f lst]
            [:li.firn-file-tag-item
             [:a.firn-file-tag-link {:href (f :from-url)} (f :from-file)]])]])])))


(defn render-org-tags
  "Renders markup for a list of tags and their respective links to org-headings. "
  [org-tags opts]
  [:div.firn-org-tags
   (when (seq org-tags)
     (for [[tag-name tags] org-tags]
       [:div.firn-org-tag-container
        [:div {:id tag-name :class "firn-org-tag-name"} tag-name]
        (for [tag tags
              :let [link (tag :headline-link)]]
          [:ul.firn-org-tag-list
           [:li.firn-org-tag-item
            [:a.firn-org-tag-link
             {:href link}
             (tag :from-file) " - " (tag :from-headline)]]])]))])

;; R: Table of Contents --------------------------------------------------


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
  ([toc {:keys [headline depth list-type exclude-headline?]
         :or   {depth nil list-type :ol}
         :as   opts}]
   (let [s-h         (u/find-first #(= (% :text) headline) toc)     ; if user specified a heading to start at, go find it.
         toc         (cond->> toc                                   ; apply some filtering to the toc, if params are passed in.
                       depth      (filter #(<= (% :level) depth))   ; if depth; keep everything under that depth.
                       headline   (drop-while #(not= s-h %))        ; drop everything up till the selected heading we want.
                       headline   (u/take-while-after-first         ; remove everything that's not a child of the selected heading.
                                   #(> (% :level) (s-h :level)))
                       exclude-headline? (drop 1))                         ; don't include selected heading; just it's children.)

         min-level   (if (seq toc) (:level (apply min-key :level toc)) 1) ; get the min level for calibrating the reduce.
         toc-cleaned (->> toc
                          (map #(assoc % :children []))  ; create a "children" key on every item.)
                          (reduce make-toc-helper-reduce {:out [] :prev nil :min-level min-level})
                          :out)]

     (if (empty? toc-cleaned) nil
         (into [list-type] (toc->html toc-cleaned list-type))))))

;; General HTML Renderers ------------------------------------------------

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
  [org-link site-url]
  (let [regex       #"(file:)(.*)\.(org)(\:\:\*.+)?"
        res         (re-matches regex org-link)
        anchor-link (last res)
        anchor-link (when anchor-link (-> res last u/clean-anchor))]
    (if anchor-link
      (str site-url "/" (nth res 2) anchor-link)
      (str site-url "/" (nth res 2)))))

(defn link->html
  "Parses links from the org-tree.
  Checks if a link is an HTTP link or File link."
  [v opts]
  (let [link-val        (get v :desc)
        link-href       (get v :path "Missing HREF attribute.")
        ;; img regexs / ctor fns.
        img-file-regex  #"(file:)(.*)\.(jpg|JPG|gif|GIF|png)"
        img-http-regex  #"(http:\/\/|https:\/\/)(.*)\.(jpg|JPG|gif|GIF|png)"
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
      (img-link->figure {:desc link-val
                         :path (str (opts :site-url) "/" (img-make-url))})

      ;; a normal http image.
      (re-matches img-http-regex link-href)
      (img-link->figure {:desc link-val :path link-href})

      ;; org files
      (re-matches org-file-regex link-href)
      [:a.firn-internal
       {:href (internal-link-handler link-href (opts :site-url))} link-val]

      (re-matches http-link-regex link-href)
      [:a.firn-external {:href link-href :target "_blank"} link-val]

      ;; Otherwise, assume it's an internal anchor link.
      :else
      [:a {:href (u/clean-anchor link-href)} link-val])))

(defn level-in-fold?
  [opts level]
  (contains? (opts :firn-fold) level))

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
        ;; this let section builds the heading elements and their respective
        ;; classes; construcing a single heading element with various children..
        heading-priority (u/str->keywrd "span.firn-headline-priority.firn-headline-priority__" priority)
        heading-keyword  (u/str->keywrd "span.firn-headline-keyword.firn-headline-keyword__" keywrd)
        heading-tags     [:span.firn-org-tags (for [t tags] [:span [:a.firn-org-tag {:href (str "/tags#" t)} t]])]
        heading-anchor   (org/make-headline-anchor parent)
        heading-is-folded (level-in-fold? opts level)
        heading-id+class #(u/str->keywrd "h" % heading-anchor ".firn-headline.firn-headline-" %
                                         (when heading-is-folded ".firn-headline-hidden"))
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
                          (if heading-is-folded
                            (make-child :span.firn-headline-text-hidden)
                            (make-child :span.firn-headline-text))
                          (when tags heading-tags)]]

    (cond
      ;; render properties
      (and properties (opts :firn-properties?))
      [:div render-headline (props->html v)]

      :else render-headline)))

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

(defn headline-fold->html
  "Handles rendering a heading+section folded in a details+summary tag."
  [v {:keys [firn-fold headline-level make-child headline-el]}]
  [(u/str->keywrd "details.firn-fold.firn-fold-" headline-level)
   {:style (str "margin-left: " (* (- headline-level 1) 12) "px")
    :open  (firn-fold headline-level)}
   [(u/str->keywrd "summary.firn-headline-summary-" headline-level) (org/get-headline-helper v)]
   (make-child headline-el)])

(defn to-html
  "Recursively Parses the org-edn into hiccup.
  Some values don't get parsed (drawers) - yet. They return empty strings.
  Don't destructure! - with recursion, it can create uneven maps from possible nil vals on `v`

  Here, `opts` is largely going to be the `config` map, with the possibility that it's values
  are overwritten with layout or file-specific settings. This happens in layout/render"
  ([v] (to-html v {}))
  ([v opts]
   (let [type           (get v :type)
         children       (get v :children)
         value          (get v :value)
         value          (if value (s/trim-newline value) value)
         ordered        (get v :ordered) ;; for lists
         headline-level (get v :level)
         ;; Since this is recursive, I wonder if performance matters for cases where we KNOW type is NOT a headline.
         headline-fold? (level-in-fold? opts headline-level)
         headline-el    (if headline-fold?
                          (u/str->keywrd "div.firn-headline-section-folded.firn-headline-section-" headline-level)
                          (u/str->keywrd "div.firn-headline-section.firn-headline-section-" headline-level))
         make-child     #(into [%] (map (fn [c] (to-html c opts)) children))
         handle-fold    #(headline-fold->html v (merge opts {:headline-level headline-level
                                                             :make-child make-child
                                                             :headline-el headline-el}))]


     (case type
       "document"      (make-child :div)
       ;; if folding is turned on for a headline, render title+section from within.
       "headline"      (if headline-fold?
                         (handle-fold)
                         (make-child headline-el))
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

