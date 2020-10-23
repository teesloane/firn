(ns firn.markup
  "Namespace responsible for converted org-edn into html."
  (:require [clojure.string :as s]
            [firn.util :as u]
            [clojure.data.priority-map :as primap]
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
  - c) we have to handle for when the user's files don't have all the necessary metadata.

  The sitemap IS a map because it enables `get-in` to render specific parts of the map.
  This, however, makes for annoying sorting issues. "
  ([sm]
   (render-site-map sm {}))
  ([sm opts]
   (let [depth-counter (atom 0)]
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

             ;; sort the sitemap, pushing nils to end of sort.
             (sort-priority-map [k smn desc?]
               (u/mapply primap/priority-map-keyfn-by (juxt #(nil? (% k)) k)
                         ;; HACK: using juxt above makes the take values x, y which are vectors;
                         ;; the first val is t/f (is it nil?), the next is the key's val, if it exists.
                         ;; we need the juxt for pushing nil values to the end of the list.
                         ;; and if we want to reverse the list AND still keep nil values at the end
                         ;; ie, to sort by ":oldest" it, seems we need to compare only by the second vec value.
                         (if desc?
                           #(compare (second %2) (second %1))
                           #(compare %1 %2)) smn))

             (sort-site-map [site-map-node] ;; site-map-node is a whole site-map or any :children sub maps.
               (case (opts :sort-by)
                 :alphabetical (into (sorted-map) site-map-node)
                 :oldest       (sort-priority-map :date-created-ts site-map-node false)
                 :newest      (sort-priority-map :date-created-ts site-map-node true) ; ????
                 :firn-order (sort-priority-map :firn-order site-map-node false)
                 site-map-node))

             ;; The recursive renderering function.
             (make-child [[k v]]
               (let [children (v :children)]
                 ;; If no children on the site map node, just render a single li element.
                 (if-not children
                   [:li (if (v :path)
                          [:a.firn-sitemap-item--link {:href (v :path)} k]
                          [:div.firn-sitemap-item--no-link k])]
                   ;; if children render recursively (unless it exceeds the optional :depth value)
                   (do
                     (swap! depth-counter inc)
                     (if (< @depth-counter (get opts :depth (+ @depth-counter 1)))
                       [:li.firn-sitemap-item--child
                        (if (v :path)
                          [:a.firn-sitemap-item--link {:href (v :path)} k]
                          [:div.firn-sitemap-item--no-link k])
                        [:ul.firn-sitemap-item--parent
                         (map make-child (sort-site-map children))]]
                       [:li (if (v :path)
                              [:a.firn-sitemap-item--link {:href (v :path)} k]
                              [:div.firn-sitemap-item--no-link k])])))))]
       (let [site-map (-> sm starting-point sort-site-map)]
         [:ul.firn-sitemap.firn-sitemap-item--parent (map make-child site-map)])))))

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
  "When rendering a file, filters the site-links for all links (that are not
  private) that link to the file."
  [{:keys [site-links file site-url site-links-private] :as opts}]
  (let [; transform site-links-private to what their url form would be.
        site-links-private       (map #(str site-url "/" %) site-links-private)
        org-path-match-file-url? #(let [site-link (internal-link-handler (% :path) opts)]
                                    (and
                                     (= site-link (file :path-url))
                                     (not (u/in? site-links-private site-link))))
        to-html                  (fn [x] [:li.firn-backlink [:a {:href (x :from-url)} (x :from-file)]])
        backlinks                (->> site-links (filter org-path-match-file-url?))
        backlinks-unique         (u/distinct-by backlinks :from-url)]
    (if (seq backlinks-unique)
      (into [:ul.firn-backlinks] (map to-html backlinks-unique))
      nil)))

;; R: Tags (firn / org)

(defn render-firn-tags
  "Renders a list of tags and their respective files
  The tag list sections renders alphabetically,

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
     (when (seq firn-tags)
       [:div.firn-file-tags
        (for [[k lst] firn-tags
              :when (not (u/in? (opts :exclude) k))]
          [:div.firn-file-tags-container
           [:div.firn-file-tag-name {:id k :class "firn-file-tag-name"} k]
           [:ul.firn-file-tag-list
            (for [f lst]
              [:li.firn-file-tag-item
               [:a.firn-file-tag-link {:href (f :from-url)} (f :from-file)]])]])]))))

(defn render-firn-file-tags
  "Renders a single list of tags for the file being rendered."
  [file-tags opts]
  (when (seq file-tags)
    [:ul.firn-file-tags
     (for [tag file-tags
           :let [href (or (str "/" (opts :firn-tags-path) "#" tag))]]
       [:li.firn-file-tag-item
        [:a.firn-file-tag-link {:href href} tag] ])]))

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

(defn render-related-files
  "For each tag in the file, get all files that fall under that tag site-wide.
  HACK: This function is not efficient."
  [curr-file-title file-tags firn-tags]
  (let [out (atom [])]
    ;; loop through the files tags
    (doseq [file-tag file-tags
            :let [related-files (get firn-tags file-tag)]]
      ;; for each file tag, get the related files from the site-wide-tags
      (doseq [f related-files
              ;; don't process the global tag for the file we are already processing.
              ;; ie, don't show the current file itself as a "related file"
              :when (not= (f :from-file) curr-file-title)]
        ;; add to the processing out atom.
        (swap! out conj f)))
    ;; loop through the final collection and render the markup.

    (if (empty? @out)
      nil
      [:ul.firn-related-files
       (for [f (u/distinct-by @out :from-file)]
         [:li.firn-related-file
          [:a.firn-related-file-link {:href (f :from-url)}
           (f :from-file)]])])))

;; R: Table of Contents --------------------------------------------------------

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

;; General HTML Renderers ------------------------------------------------------

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
  [{:keys [contents language _arguments] :as _src-block}]
  [(u/str->keywrd "pre.language-" language)
   [(u/str->keywrd "code.language-" language) contents]])

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
  [org-link {:keys [site-url]}]
  (let [{:keys [anchor slug]} (org/get-link-parts org-link)]
    (if anchor
      (str site-url "/" slug anchor)
      (str site-url "/" slug))))

(defn link->html
  "Parses links from the org-tree.
  Checks if a link is an HTTP link or File link."
  [v opts]
  (let [link-val       (get v :desc)
        link-href      (get v :path "Missing HREF attribute.")
        ;; img regexs / ctor fns.
        img-file-regex #"(file:)(.*)(\.(jpg|JPG|gif|GIF|png|PNG|jpeg|JPEG|svg|SVG))"
        img-http-regex #"(http:\/\/|https:\/\/)(.*)\.(jpg|JPG|gif|GIF|png|PNG|jpeg|JPEG|svg|SVG)"
        mailto-regex   #"(mailto:)(.*)"
        ;; file regexs / ctor fns
        org-file-regex  #"(file:)(.*)\.(org)(\:\:\*.+)?"
        ext-file-regex  #"(file:)(.*)(\.[^.]+$)" ; match any: file:my_file.biz
        http-link-regex #"https?:\/\/(?![^\" ]*(?:jpg|png|gif))[^\" ]+"
        ;; breaks link into a list and cleans it: `file:my_img.jpg` => `my_img.jpg`
        attach-make-url #(->> (re-matches % link-href)
                              (drop 2)
                              (take 2)
                              (s/join ""))]

    ;; I wonder if pattern matching makes more sense here.
    (cond
      ;; Images ---
      ;; img file or attach: `file:`
      (re-matches img-file-regex link-href)
      (img-link->figure {:desc link-val
                         :path (str (opts :site-url) "/" (attach-make-url img-file-regex))})

      ;; a normal http image.
      (re-matches img-http-regex link-href)
      (img-link->figure {:desc link-val :path link-href})

      ;; org files (if it's not a private link.)
      (re-matches org-file-regex link-href)
      (let [{:keys [slug]} (org/get-link-parts link-href)
            is-priv-link?  (u/in? (opts :site-links-private) slug)]
        (if is-priv-link?
          [:span.firn-link-disabled link-val]
          [:a.firn-internal
           {:href (internal-link-handler link-href opts)} link-val]))

      (re-matches http-link-regex link-href)
      [:a.firn-external {:href link-href :target "_blank"} link-val]

      ;; a mail link
      (re-matches mailto-regex link-href)
      [:a.firn-mail
       {:href link-href} link-val]

      (re-matches ext-file-regex link-href)
      [:a.firn-internal-file {:href (str (opts :site-url) "/" (attach-make-url ext-file-regex))} link-val]
      
      ;; Otherwise, assume it's an internal anchor link.
      :else
      [:a {:href (u/clean-anchor link-href)} link-val])))

;; Headlines --

(defn level-in-fold?
  [opts level]
  (contains? (opts :firn-fold) level))

(defn handle-headline
  "Determines if and how a headline should be rendered
  - renders folded or not
  - does not render if healine is tagged with :noexport:"
  [v opts make-child]
  (let [headline-level (get v :level)
        headline-fold? (level-in-fold? opts headline-level)
        firn-fold      (opts :firn-fold)
        headline-el    (if headline-fold?
                         (u/str->keywrd "div.firn-headline-section-folded.firn-headline-section-" headline-level)
                         (u/str->keywrd "div.firn-headline-section.firn-headline-section-" headline-level))]

    (when-not (org/headline-exported? v)
      (if-not headline-fold?
        (make-child headline-el)
        ;; Render it folded.
        [(u/str->keywrd "details.firn-fold.firn-fold-" headline-level)
         {:open  (firn-fold headline-level)}
         [(u/str->keywrd "summary.firn-headline-summary-" headline-level) (org/get-headline-helper v)]
         (make-child headline-el)]))))

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
        tag-link         (or (str "/" (opts :org-tags-path) "#") "/tags#")
        heading-priority (u/str->keywrd "span.firn-headline-priority.firn-headline-priority__" priority)
        heading-keyword  (u/str->keywrd "span.firn-headline-keyword.firn-headline-keyword__" keywrd)

        heading-tags     [:span.firn-org-tags (for [t tags] [:span [:a.firn-org-tag {:href (str tag-link t)} t]])]
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
         ;; Since this is recursive, I wonder if performance matters for cases where we KNOW type is NOT a headline.
         make-child     #(into [%] (map (fn [c] (to-html c opts)) children))]

     (case type
       "document"      (make-child :div)
       ;; if folding is turned on for a headline, render title+section from within.
       "headline"      (handle-headline v opts make-child)
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
