(ns firn.markup
  "Namespace responsible for converted org-edn into html."
  (:require [clojure.string :as s]
            [firn.util :as u]
            [firn.org :as org]))

(declare to-html)

;; Render: Site-map ------------------------------------------------------------

(defn render-site-map
  "Converts the site map data structure into html. Takes options for sorting."
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
                   [(get-in smn [k1 prop]) k1]
                   [(get-in smn [k2 prop]) k2])))))

           ;; TODO - test this more.
           (starting-point [sm]
             (if (opts :start-at)
               (get-in sm (u/interpose+tail (opts :start-at) :children))
               sm))

           (sort-site-map [site-map-node] ;; site-map-node is a whole site-map or any :children sub maps.
             (case (opts :sort-by)
               "alphabetical" (into (sorted-map) site-map-node)
               "newest"       (into (sorted-map-by (sort-by-key site-map-node :date-created-ts true)) site-map-node)
               "oldest"       (into (sorted-map-by (sort-by-key site-map-node :date-created-ts)) site-map-node)
               ;; TODO: updated doesn't seem to be working yet.
               ;; "updated"      (into (sorted-map-by (sort-by-key site-map-node :date-updated-ts )) site-map-node)
               "order"        (into (sorted-map-by (sort-by-key site-map-node :firn-order)) site-map-node)
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
       [:ul.firn-sitemap (map make-child starting-site-map)]))))

;; Render: Table of Contents --------------------------------------------------

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
       {:href (str (opts :site-url) (internal-link-handler link-href))} link-val]

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
        heading-tags     [:span.firn-tags (for [t tags] [:span [:a.firn-tag {:href (str "/tags#" t)} t]])]
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
  Don't destructure! - with recursion, it can create uneven maps from possible nil vals on `v`"
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


(def x {"Objects" {:children {"Camera" {:path "http://localhost:4000/camera",
                                        :date-created "2020-04-09 Thu",
                                        :date-updated "2020-08-04 20:48",
                                        :firn-under ["Objects"],
                                        :date-created-ts 1586404800,
                                        :title "Camera",
                                        :firn-order 1,
                                        :date-updated-ts 1586404800,
                                        :logbook-total "0:00"}}},
        "Index" {:path "http://localhost:4000/index",
                 :date-created "2020-03-23 12:00",
                 :date-updated "2020-07-06 08:38",
                 :firn-under nil,
                 :date-created-ts 1584936000,
                 :title "Index",
                 :firn-order nil,
                 :date-updated-ts 1584936000,
                 :logbook-total "0:00"},
        "Writing" {:path "http://localhost:4000/writing",
                   :children {"Bad Time Time Capsule" {:path "http://localhost:4000/bad_time_time_capsule",
                                                       :date-created "2019-08-19 07:32",
                                                       :date-updated "2020-08-07 10:11",
                                                       :firn-under ["Writing"],
                                                       :date-created-ts 1566187200,
                                                       :title "Bad Time Time Capsule",
                                                       :firn-order nil,
                                                       :date-updated-ts 1566187200,
                                                       :logbook-total "20:44"}},
                   :date-created "2020-05-04 15:10",
                   :date-updated "2020-08-05 10:44",
                   :firn-under nil,
                   :date-created-ts 1588564800,
                   :title "Writing",
                   :firn-order 2,
                   :date-updated-ts 1588564800,
                   :logbook-total "0:00"},
        "Log" {:children {"First Game Jam" {:path "http://localhost:4000/log/2020-04-19-1421-first_game_jam",
                                            :date-created "2020-04-19 Sun",
                                            :date-updated "2020-05-31 15:14",
                                            :firn-under ["Log"],
                                            :date-created-ts 1587268800,
                                            :title "First Game Jam",
                                            :firn-order nil,
                                            :date-updated-ts 1587268800,
                                            :logbook-total "0:00"},
                          "Digging for Crystals" {:path "http://localhost:4000/log/2020-06-06-1747-digging_for_crystals",
                                                  :date-created "2020-06-06 Sat",
                                                  :date-updated "2020-06-14 08:36",
                                                  :firn-under ["Log"],
                                                  :date-created-ts 1591416000,
                                                  :title "Digging for Crystals",
                                                  :firn-order nil,
                                                  :date-updated-ts 1591416000,
                                                  :logbook-total "0:00"},
                          "Assimil - 1st Wave" {:path "http://localhost:4000/log/2020-05-09-1128-assimil_1st_wave",
                                                :date-created "2020-05-09 Sat",
                                                :date-updated "2020-06-17 13:41",
                                                :firn-under ["Log"],
                                                :date-created-ts 1588996800,
                                                :title "Assimil - 1st Wave",
                                                :firn-order nil,
                                                :date-updated-ts 1588996800,
                                                :logbook-total "0:00"},
                          "Firn Progress Report" {:path "http://localhost:4000/log/2020-04-30-1020-firn_progress_report",
                                                  :date-created "2020-04-30 Thu",
                                                  :date-updated "2020-06-20 07:55",
                                                  :firn-under ["Log"],
                                                  :date-created-ts 1588219200,
                                                  :title "Firn Progress Report",
                                                  :firn-order nil,
                                                  :date-updated-ts 1588219200,
                                                  :logbook-total "0:00"},
                          "Rediscovering Ruby" {:path "http://localhost:4000/log/2020-04-29-1637-rediscovering_ruby",
                                                :date-created "2020-04-29 Wed",
                                                :date-updated "2020-06-18 07:28",
                                                :firn-under ["Log"],
                                                :date-created-ts 1588132800,
                                                :title "Rediscovering Ruby",
                                                :firn-order nil,
                                                :date-updated-ts 1588132800,
                                                :logbook-total "0:00"}}},
        "Travel" {:children {"Zion" {:path "http://localhost:4000/zion",
                                     :date-created "2020-05-12 20:42",
                                     :date-updated "2020-08-04 20:53",
                                     :firn-under ["Travel"],
                                     :date-created-ts 1589256000,
                                     :title "Zion",
                                     :firn-order nil,
                                     :date-updated-ts 1589256000,
                                     :logbook-total "0:00"},
                             "Vegas" {:path "http://localhost:4000/vegas",
                                      :date-created "2019-06-06 Thu",
                                      :date-updated "2020-08-04 16:48",
                                      :firn-under ["Travel"],
                                      :date-created-ts 1559793600,
                                      :title "Vegas",
                                      :firn-order nil,
                                      :date-updated-ts 1559793600,
                                      :logbook-total "0:00"}}},
        "Navigation" {:children {"Now" {:path "http://localhost:4000/now",
                                        :date-created "2020-05-21 Thu",
                                        :date-updated "2020-08-02 09:55",
                                        :firn-under ["Navigation"],
                                        :date-created-ts 1590033600,
                                        :title "Now",
                                        :firn-order nil,
                                        :date-updated-ts 1590033600,
                                        :logbook-total "0:00"},
                                 "Indelible" {:path "http://localhost:4000/indelible",
                                              :date-created "2020-02-28 19:31",
                                              :date-updated "2020-08-04 20:52",
                                              :firn-under ["Navigation"],
                                              :date-created-ts 1582866000,
                                              :title "Indelible",
                                              :firn-order nil,
                                              :date-updated-ts 1582866000,
                                              :logbook-total "0:00"}}},
        "The Ice Shelf" {:path "http://localhost:4000/the-ice-shelf",
                         :date-created "2020-02-28 20:56",
                         :date-updated "2020-05-31 18:59",
                         :firn-under nil,
                         :date-created-ts 1582866000,
                         :title "The Ice Shelf",
                         :firn-order nil,
                         :date-updated-ts 1582866000,
                         :logbook-total "0:00"},
        "Projects" {:children {"House of the Lake" {:path "http://localhost:4000/house_of_the_lake",
                                                    :date-created "2019-11-01",
                                                    :date-updated "2020-08-04 20:51",
                                                    :firn-under ["Projects"],
                                                    :date-created-ts 1572580800,
                                                    :title "House of the Lake",
                                                    :firn-order nil,
                                                    :date-updated-ts 1572580800,
                                                    :logbook-total "56:53"},
                               "Pod-Dodger" {:path "http://localhost:4000/pod-dodger",
                                             :date-created "2020-03-08 20:03",
                                             :date-updated "2020-08-04 20:58",
                                             :firn-under ["Projects"],
                                             :date-created-ts 1583643600,
                                             :title "Pod-Dodger",
                                             :firn-order nil,
                                             :date-updated-ts 1583643600,
                                             :logbook-total "11:20"},
                               "A Short Poem Rings Out" {:path "http://localhost:4000/a-short-poem-rings-out",
                                                         :date-created "2020-02-24 21:16",
                                                         :date-updated "2020-08-04 21:01",
                                                         :firn-under ["Projects"],
                                                         :date-created-ts 1582520400,
                                                         :title "A Short Poem Rings Out",
                                                         :firn-order nil,
                                                         :date-updated-ts 1582520400,
                                                         :logbook-total "4:31"},
                               "Railcar" {:path "http://localhost:4000/railcar",
                                          :date-created "2019-01-12 Sat",
                                          :date-updated "2020-08-04 20:59",
                                          :firn-under ["Projects"],
                                          :date-created-ts 1547269200,
                                          :title "Railcar",
                                          :firn-order nil,
                                          :date-updated-ts 1547269200,
                                          :logbook-total "0:00"},
                               "Laundromat" {:path "http://localhost:4000/laundromat",
                                             :date-created "2016-05-15 Sun",
                                             :date-updated "2020-08-07 10:17",
                                             :firn-under ["Projects"],
                                             :date-created-ts 1463284800,
                                             :title "Laundromat",
                                             :firn-order nil,
                                             :date-updated-ts 1463284800,
                                             :logbook-total "2:35"},
                               "Wet Dog" {:path "http://localhost:4000/wet-dog",
                                          :date-created "2020-02-25 08:44",
                                          :date-updated "2020-08-04 21:02",
                                          :firn-under ["Projects"],
                                          :date-created-ts 1582606800,
                                          :title "Wet Dog",
                                          :firn-order nil,
                                          :date-updated-ts 1582606800,
                                          :logbook-total "10:18"},
                               "Click Shape" {:path "http://localhost:4000/click-shape",
                                              :date-created "2019-08-08",
                                              :date-updated "2020-08-04 20:48",
                                              :firn-under ["Projects"],
                                              :date-created-ts 1565236800,
                                              :title "Click Shape",
                                              :firn-order nil,
                                              :date-updated-ts 1565236800,
                                              :logbook-total "17:44"},
                               "Meta" {:path "http://localhost:4000/meta",
                                       :date-created "2020-04-01 Wed",
                                       :date-updated "2020-08-07 10:22",
                                       :firn-under ["Projects"],
                                       :date-created-ts 1585713600,
                                       :title "Meta",
                                       :firn-order nil,
                                       :date-updated-ts 1585713600,
                                       :logbook-total "13:32"},
                               "Construction (Priest)" {:path "http://localhost:4000/construction-priest",
                                                        :date-created "2020-02-24 21:16",
                                                        :date-updated "2020-08-04 20:49",
                                                        :firn-under ["Projects"],
                                                        :date-created-ts 1582520400,
                                                        :title "Construction (Priest)",
                                                        :firn-order nil,
                                                        :date-updated-ts 1582520400,
                                                        :logbook-total "11:54"},
                               "Ari's Garden" {:path "http://localhost:4000/aris-garden",
                                               :date-created "2019-08-19 11:30",
                                               :date-updated "2020-08-04 20:46",
                                               :firn-under ["Projects"],
                                               :date-created-ts 1566187200,
                                               :title "Ari's Garden",
                                               :firn-order nil,
                                               :date-updated-ts 1566187200,
                                               :logbook-total "113:56"},
                               "Neve" {:path "http://localhost:4000/neve",
                                       :date-created "2020-02-25 08:44",
                                       :date-updated "2020-08-04 20:52",
                                       :firn-under ["Projects"],
                                       :date-created-ts 1582606800,
                                       :title "Neve",
                                       :firn-order nil,
                                       :date-updated-ts 1582606800,
                                       :logbook-total "2:42"},
                               "Firn" {:path "http://localhost:4000/firn",
                                       :date-created "2020-03-01 09:53",
                                       :date-updated "2020-08-07 10:17",
                                       :firn-under ["Projects"],
                                       :date-created-ts 1583038800,
                                       :title "Firn",
                                       :firn-order nil,
                                       :date-updated-ts 1583038800,
                                       :logbook-total "192:40"}}},
        "Research" {:path "http://localhost:4000/research",
                    :children {"Bread" {:path "http://localhost:4000/bread",
                                        :date-created "2020-05-20 19:21",
                                        :date-updated "2020-08-04 20:47",
                                        :firn-under ["Research"],
                                        :date-created-ts 1589947200,
                                        :title "Bread",
                                        :firn-order nil,
                                        :date-updated-ts 1589947200,
                                        :logbook-total "0:00"},
                               "Quil" {:path "http://localhost:4000/quil",
                                       :date-created "2020-02-28 08:31",
                                       :date-updated "2020-08-04 20:59",
                                       :firn-under ["Research"],
                                       :date-created-ts 1582866000,
                                       :title "Quil",
                                       :firn-order nil,
                                       :date-updated-ts 1582866000,
                                       :logbook-total "0:00"},
                               "Generative Art" {:path "http://localhost:4000/generative_art",
                                                 :date-created "2020-06-02 Tue",
                                                 :date-updated "2020-08-04 20:51",
                                                 :firn-under ["Research"],
                                                 :date-created-ts 1591070400,
                                                 :title "Generative Art",
                                                 :firn-order nil,
                                                 :date-updated-ts 1591070400,
                                                 :logbook-total "0:00"},
                               "Anki" {:path "http://localhost:4000/anki",
                                       :date-created "2020-03-30",
                                       :date-updated "2020-08-04 20:46",
                                       :firn-under ["Research"],
                                       :date-created-ts 1585540800,
                                       :title "Anki",
                                       :firn-order nil,
                                       :date-updated-ts 1585540800,
                                       :logbook-total "0:00"},
                               "Org Mode" {:path "http://localhost:4000/org-mode",
                                           :date-created "2020-02-28 20:56",
                                           :date-updated "2020-08-04 21:02",
                                           :firn-under ["Research"],
                                           :date-created-ts 1582866000,
                                           :title "Org Mode",
                                           :firn-order nil,
                                           :date-updated-ts 1582866000,
                                           :logbook-total "0:00"},
                               "Open Frameworks" {:path "http://localhost:4000/open_frameworks",
                                                  :date-created "2020-07-08 15:48",
                                                  :date-updated "2020-08-06 17:35",
                                                  :firn-under ["Research"],
                                                  :date-created-ts 1594180800,
                                                  :title "Open Frameworks",
                                                  :firn-order nil,
                                                  :date-updated-ts 1594180800,
                                                  :logbook-total "0:00"},
                               "Cycling" {:path "http://localhost:4000/cycling",
                                          :date-created "2020-02-28 19:31",
                                          :date-updated "2020-08-04 21:09",
                                          :firn-under ["Research"],
                                          :date-created-ts 1582866000,
                                          :title "Cycling",
                                          :firn-order nil,
                                          :date-updated-ts 1582866000,
                                          :logbook-total "0:00"},
                               "French" {:path "http://localhost:4000/french",
                                         :date-created "2020-02-24 21:16",
                                         :date-updated "2020-08-06 10:19",
                                         :firn-under ["Research"],
                                         :date-created-ts 1582520400,
                                         :title "French",
                                         :firn-order nil,
                                         :date-updated-ts 1582520400,
                                         :logbook-total "148:02"},
                               "Clojure" {:path "http://localhost:4000/clojure",
                                          :date-created "2020-02-28",
                                          :date-updated "2020-08-04 20:49",
                                          :firn-under ["Research"],
                                          :date-created-ts 1582866000,
                                          :title "Clojure",
                                          :firn-order nil,
                                          :date-updated-ts 1582866000,
                                          :logbook-total "0:00"},
                               "Processing" {:path "http://localhost:4000/processing",
                                             :date-created "2020-08-05 Wed",
                                             :date-updated "2020-08-05 15:44",
                                             :firn-under ["Research"],
                                             :date-created-ts 1596600000,
                                             :title "Processing",
                                             :firn-order nil,
                                             :date-updated-ts 1596600000,
                                             :logbook-total "0:00"},
                               "Assimil" {:path "http://localhost:4000/assimil",
                                          :date-created "2020-02-25 05:51",
                                          :date-updated "2020-08-05 19:07",
                                          :firn-under ["Research"],
                                          :date-created-ts 1582606800,
                                          :title "Assimil",
                                          :firn-order nil,
                                          :date-updated-ts 1582606800,
                                          :logbook-total "0:00"},
                               "Tic-80" {:path "http://localhost:4000/tic_80",
                                         :date-created "2020-04-09 Thu",
                                         :date-updated "2020-08-05 19:07",
                                         :firn-under ["Research"],
                                         :date-created-ts 1586404800,
                                         :title "Tic-80",
                                         :firn-order nil,
                                         :date-updated-ts 1586404800,
                                         :logbook-total "0:00"},
                               "Language" {:path "http://localhost:4000/language",
                                           :date-created "2020-03-05 08:16",
                                           :date-updated "2020-08-04 20:58",
                                           :firn-under ["Research"],
                                           :date-created-ts 1583384400,
                                           :title "Language",
                                           :firn-order nil,
                                           :date-updated-ts 1583384400,
                                           :logbook-total "0:00"},
                               "Coffee" {:path "http://localhost:4000/coffee",
                                         :date-created "2020-04-02",
                                         :date-updated "2020-08-04 20:49",
                                         :firn-under ["Research"],
                                         :date-created-ts 1585800000,
                                         :title "Coffee",
                                         :firn-order nil,
                                         :date-updated-ts 1585800000,
                                         :logbook-total "0:00"},
                               "Code Snippets" {:path "http://localhost:4000/code_snippets",
                                                :date-created "2020-08-02 Sun",
                                                :date-updated "2020-08-04 20:49",
                                                :firn-under ["Research"],
                                                :date-created-ts 1596340800,
                                                :title "Code Snippets",
                                                :firn-order nil,
                                                :date-updated-ts 1596340800,
                                                :logbook-total "0:00"},
                               "Emacs" {:path "http://localhost:4000/emacs",
                                        :date-created "2020-02-28 19:31",
                                        :date-updated "2020-08-04 20:50",
                                        :firn-under ["Research"],
                                        :date-created-ts 1582866000,
                                        :title "Emacs",
                                        :firn-order nil,
                                        :date-updated-ts 1582866000,
                                        :logbook-total "0:00"}},
                    :date-created "2020-08-07 Fri",
                    :date-updated "2020-08-07 10:21",
                    :firn-under nil,
                    :date-created-ts 1596772800,
                    :title "Research",
                    :firn-order nil,
                    :date-updated-ts 1596772800,
                    :logbook-total "0:00"},
        "Cafes" {:path "http://localhost:4000/cafes",
                 :date-created "2019-03-02 21:16",
                 :date-updated "2020-08-07 10:27",
                 :firn-under nil,
                 :date-created-ts 1551502800,
                 :title "Cafes",
                 :firn-order nil,
                 :date-updated-ts 1551502800,
                 :logbook-total "0:00"}})
