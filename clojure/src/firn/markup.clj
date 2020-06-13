(ns firn.markup
  "Namespace responsible for converted org-edn into html."
  (:require [clojure.string :as s]
            [firn.util :as u]
            [firn.org :as org]
            [hiccup.core :as h]))

(declare to-html)

(def x [{:level 1, :raw "Meta", :anchor "#meta"}
        {:level 2, :raw "Resources", :anchor "#resources"}
        {:level 3, :raw "Third level", :anchor "#third-level"}
        {:level 4, :raw "fourth", :anchor "#fouth-level"}
        {:level 4, :raw "fourth 22", :anchor "#fouth-level"}
        {:level 4, :raw "fourth 22", :anchor "#fouth-level"}
        {:level 4, :raw "fourth 22", :anchor "#fouth-level"}
        {:level 4, :raw "fourth 22", :anchor "#fouth-level"}
        {:level 2, :raw "back to 2", :anchor "#back-to-2-level"}])
        ;; {:level 4, :raw "Notes", :anchor "#notes"}])

(def y [{:level 1, :raw "Meta", :anchor "#meta"}
        {:level 1, :raw "Assimil", :anchor "#assimil"}
        {:level 1, :raw "Resources", :anchor "#resources"}
        {:level 1, :raw "Current readings", :anchor "#current-readings"}
        {:level 2, :raw "Around the world in 80 days", :anchor "#around-the-world-in-80-days"}
        {:level 1, :raw "Notes", :anchor "#notes"}
        {:level 2, :raw "Preface", :anchor "#preface"}
        {:level 2, :raw "Current Tools", :anchor "#current-tools"}
        {:level 2, :raw "Nouns", :anchor "#nouns"}
        {:level 2, :raw "Adjectives", :anchor "#adjectives"}
        {:level 2, :raw "Adverbs", :anchor "#adverbs"}
        {:level 2, :raw "Verbs (families)", :anchor "#verbs-(families)"}
        {:level 2, :raw "Prepositions", :anchor "#prepositions"}
        {:level 2, :raw "Verb Tenses and Moods [0/5]", :anchor "#verb-tenses-and-moods"}
        {:level 3, :raw "Présent (The present tense) [0/0]", :anchor "#présent-(the-present-tense)"}
        {:level 3, :raw "Le présent progressif (The /very/ present tense)", :anchor "#le-présent-progressif-(the-present-tense)"}
        {:level 3, :raw "Le passé récent (The /just happened/ tense)", :anchor "#le-passé-récent-(the-tense)"}
        {:level 3, :raw "The Passé Composé (The \"Composed Past\")", :anchor "#the-passé-composé-(the-\"composed-past\")"}
        {:level 4, :raw "With Avoir", :anchor "#with-avoir"}
        {:level 4, :raw "With Être", :anchor "#with-être"}
        {:level 4, :raw "Exceptions and Irregularities", :anchor "#exceptions-and-irregularities"}
        {:level 3, :raw "L'imparfait (The Imperfect Tense )", :anchor "#l'imparfait-(the-imperfect-tense-)"}
        {:level 3, :raw "The Present Participle (Gerund - \"=ing\"=)", :anchor "#the-present-participle-(gerund---\"-)"}
        {:level 3, :raw "Futur Proche (The /Near/ Future - =aller= auxilliary)", :anchor "#futur-proche-(the-future---auxilliary)"}
        {:level 3, :raw "Futur Simple (The Simple Future)", :anchor "#futur-simple-(the-simple-future)"}
        {:level 3, :raw "Verb Moods [0/4]", :anchor "#verb-moods"}
        {:level 4, :raw "Conditionnel (The Conditional)", :anchor "#conditionnel-(the-conditional)"}
        {:level 4, :raw "Subjonctif (The Subjunctive)", :anchor "#subjonctif-(the-subjunctive)"}
        {:level 4, :raw "L'impératif (Imperative)", :anchor "#l'impératif-(imperative)"}
        {:level 4, :raw "Indicatif (Indicative - most common?)", :anchor "#indicatif-(indicative---most-common?)"}
        {:level 4, :raw "Participe (Participle)", :anchor "#participe-(participle)"}
        {:level 4, :raw "Infinitif (Infinitive)", :anchor "#infinitif-(infinitive)"}
        {:level 2, :raw "Idioms, Slang, and Expressions", :anchor "#idioms,-slang,-and-expressions"}
        {:level 3, :raw "Idioms", :anchor "#idioms"}
        {:level 3, :raw "Slang", :anchor "#slang"}
        {:level 3, :raw "Expressions", :anchor "#expressions"}
        {:level 2, :raw "\"False Friends\"", :anchor "#\"false-friends\""}
        {:level 2, :raw "Uncategorized Notes", :anchor "#uncategorized-notes"} {:level 3, :raw "~Ce~ vs. ~cette~", :anchor "#ce-vs.-cette"} {:level 3, :raw "Difference Between ~c'est~ and ~il est~", :anchor "#difference-between-c'est-and-il-est"} {:level 3, :raw "Meaning of ~en~ (assimil definition)", :anchor "#meaning-of-en-(assimil-definition)"} {:level 3, :raw "Reflective verbs (me, se, te)", :anchor "#reflective-verbs-(me,-se,-te)"} {:level 3, :raw "Use of ~y~, such as ~il y a~", :anchor "#use-of-y-,-such-as-il-y-a"} {:level 3, :raw "\"Not have that\" vs \"only have\":", :anchor "#\"not-have-that\"-vs-\"only-have\":"} {:level 3, :raw "S'agir de", :anchor "#s'agir-de"} {:level 3, :raw "Falloir (To Have To)", :anchor "#falloir-(to-have-to)"} {:level 3, :raw "Usage of =tout= and =tous=", :anchor "#usage-of-and"} {:level 2, :raw "Footnotes", :anchor "#footnotes"}])

;; Feature: Table of Contents --------------------------------------------------

(defn make-toc-helper-reduce
  "(ಥ﹏ಥ) Yeah. So. See the docstring for make-toc.
  Basically, this is a bit of a nightmare. This turns a flat list into a tree
  So that we can property create nested table of contents."
  [{:keys [out prev min-level] :as acc} curr]
  (prn "curr is ")(clojure.pprint/pprint curr)
  (prn "prev is: ")(clojure.pprint/pprint prev)
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

    ;; TODO - not yet handling climbing back out of multiple children buckets into the parent.
    (< (curr :level) (prev :level))
    (let [
          difference   (- (prev :level) (curr :level))                 ; if we are on level 5, and the next is level 3...
          diff-to-take (* difference 2)                                ; we need to take (5 - 3 ) * 2 = 4 items off the last :next-sibling
          ;; HACK: we can use the prev-elements :next-sibling path and chop N elements off the ending based on our heading; which gives us the path to conj onto.
          path         (vec (drop-last diff-to-take (prev :next-sibling)))
          parent-path  (count (get-in acc path))
          with-meta    (assoc curr :next-sibling path) ;; if more, add children, if equal, conj onto children.
          with-meta    (assoc with-meta :next-child (conj path parent-path :children))]

      ;; (prn "WE ARE HERE IT IS HAPPENING" (curr :level) (prev :level) "path is " path "before it was " (prev :next-sibling))
      ;; LEAVING OFF - this doesn't reliably put things back into parent.
      (-> acc
         (update-in path conj with-meta)
         (assoc :prev with-meta)))
    ;; TODO - still need to re-add the with-meta.
    :else
    (do
      (println "Something has gone wrong.")
      acc)))

(defn toc->html
  [toc kind]
  (->> toc
     (map (fn [x]
            (if (empty? (x :children))
              [:li
               [:a {:href (x :anchor)} (x :raw)]]
              [:li
               [:a {:href (x :anchor)} (x :raw)]
               [kind (toc->html (x :children) kind)]])))))

(defn make-toc
  "toc: a flattened list of headlines with a :level value of 1-> N:
  [{:level 1, :raw 'Process', :anchor '#process'}  {:level 2, :raw 'Relevance', :anchor '#relevance'}]

  We conditonally thread the heading, passing in configured values, such as
  where to start the table of contents (at a specific headline?)
  or unto what depth we want the headings to render."
  ([toc]
   (make-toc toc {}))
  ([toc {:keys [headline depth list-type]
         :or   {depth nil list-type :ol}
         :as   opts}]
   ;; (prn "toc" toc)
   (let [starting-heading   (u/find-first #(= (% :raw) headline) toc)
         h>starting-heading #(> (% :level) (starting-heading :level))
         h<depth            #(< (% :level) depth)
         just-depth         (and depth (nil? headline))
         toc-cleaned        (cond->> toc
                              ;; if we pass in a heading...
                              headline             (drop-while #(not= starting-heading %)) ; drop all headings till matching the starting-heading
                              headline             (rest)                                  ; Don't count the first heading
                              (and headline depth) (take-while #(and (h>starting-heading %) (h<depth %)))
                              just-depth           (filter #(< (% :level) depth))
                              true                 (map #(assoc % :children [])) ;; create a "children" key on every item.
                              true                 (reduce make-toc-helper-reduce {:out [] :prev nil :min-level 1}) ;; TODO - min level here needs to be calculated with min-key
                              true                 :out)]


     (if (empty? toc-cleaned) nil
         ;; toc-cleaned
         (into [list-type] (toc->html toc-cleaned list-type))))))

;; (spit "/Users/tees/Desktop/blab.html" (h/html (make-toc y)))
;; (make-toc y)

;;
;; General Renderers -----------------------------------------------------------

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
  [:pre]
  [:code contents])

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
