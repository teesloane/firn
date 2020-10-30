(ns firn.org
  "The org namespace handles all data-related to the parsing of an org file.
  When an org file is parsed it is organized into a map of data.

  Functions are either associated with:
  - a) parsing and organizing a new file into data
  - b) querying the parsed file to determine various things such as:
    - is the file private?
    - is this headline exported?

  "
  (:require [clojure.java.shell :as sh]
            [clojure.string :as s]
            [firn.util :as u]
            [sci.core :as sci]
            [cheshire.core :as json])
  (:import iceshelf.clojure.rust.ClojureRust))

;; -- Consts ----
;;
;; #+ORG_KEYWORDS that we want to evaluate into real clojure values.
(def keywords-to-eval [:firn-toc :firn-properties? :firn-order :firn-fold :firn-sitemap? :firn-private])

;; -- Data ----

(defn empty-file
  "The shape of a new org file.
  Describes what an org file will look like after it's parsed."
  []
  {:as-edn   nil ; JSON of org file -> converted to a map.
   :as-html  nil ; the html output
   :as-json  nil ; The org file, read as json and spat out by the rust binary.
   :keywords nil ; list of keywords at top of org file: #+TITLE:, #+CATEGORY, etc.
   :name     ""  ; the file name
   :path     ""  ; dir path to the file.
   :meta     {}  ; is filled when process-file / extract-metadata is run.
   :path-web ""  ; path to file from cwd: some/dirs/to/the/file - not well named.
   :path-url ""
   :original nil}
  )

;; -- Parsing ----

(defn parse!
  "Parse the org-mode file-string.
  When developing with a REPL, this shells out to the rust bin.
  When compiled to a native image, it uses JNI to talk to the rust .dylib."
  [file-str]
  (if (u/native-image?)
    (ClojureRust/getFreeMemory file-str) ;; TODO: get free memory should be renamed to "parse-org" or something else.
    (let [parser   (str (u/get-cwd) "/resources/parser")
          stripped (s/trim-newline file-str)
          res      (sh/sh parser stripped)]
      (if-not (= (res :exit) 0)
        (prn "Orgize failed to parse file." stripped res)
        (res :out)))))

(defn parse-dev!
  "DevXp func: Useful for testing org strings in the repl."
  [s]
  (let [parser   (str (u/get-cwd) "/resources/parser")
        stripped (s/trim-newline s)
        res      (sh/sh parser stripped)]
    (if-not (= (res :exit) 0)
      (prn "Orgize failed to parse file." stripped res)
      (json/parse-string (res :out) true))))

(defn parsed-org-date->unix-time
  "Converts the parsed org date (ex: [2020-04-27 Mon 15:39] -> 1588003740000)
  and turns it into a unix timestamp."
  [{:keys [year month day hour minute]} file-name]

  (let [pod->str    (str year "-" month "-" day "T" hour ":" minute ":00.000-0000")
        sdf         (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSSZ")]
    (try
      (.getTime (.parse sdf pod->str))
      (catch Exception e
        (u/print-err! :warning  (str "Failed to parse the logbook for file:" "<<" file-name ">>" "\nThe logbook may be incorrectly formatted.\nError value:" e))
        "???"))))

;; -- Getters ----
;; Functions that pull data out of the org edn structure.

(defn get-headline-helper
  "Sanitizes a heading of links and just returns text.
  Necessary because org leafs of :type `link` have a `:desc` and not a `:value`

  Turns: `* This is my [[Headline]] with a link!` -> `This is my Headline with a link!`

  UGH. this is a mess of a function, and should be refactored. Basically:
  - Loop through the children that represent a title.
  - get the values out and trim them in case there is whitespace
  (between say, a cookie or a priority, or empty text)
  - Then, filter out all empty strings
  - Then join them together.

  All in all, this is supposed to remove the dynamic elements of a heading so user's don't have to
  search headlines that have percentages, or priorities, etc."
  [headline]
  (let [title-children  (-> headline :children first :children)
        get-trimmed-val #(let [trimmed-val (s/trim (get %1 %2 ""))] ;; NOTE: not sure if should default to ""
                           (if (empty? trimmed-val) "" trimmed-val))]
    (s/join " "
            (filter not-empty
                    (for [child title-children]
                      (s/trim
                       (case (:type child)
                         "text"     (get-trimmed-val child :value)
                         "link"     (get-trimmed-val child :desc)
                         "code"     (get-trimmed-val child :value)
                         "verbatim" (get-trimmed-val child :value)
                         "")))))))

(defn get-headline
  "Fetches a headline from an org-mode tree."
  [tree name]
  (->> (tree-seq map? :children tree)
       (filter #(and (= "headline" (:type %))
                     (= name (get-headline-helper %))))
       (first)))

(defn get-headline-content
  "Same as get-headline, but removes the first child :title)."
  [tree name]
  (let [headline (get-headline tree name)]
    (update headline :children (fn [d] (filter #(not= (:type %) "title") d)))))

(defn get-headline-tags
  "Takes a headline structure and returns it's tags."
  [hl]
  (-> hl :children first :tags))

(defn get-frontmatter [f]
  (-> f :meta :keywords))

(defn get-firn-tags
  "Gets tags out of the front matter
  Exects key"
  [file]
  (let [fm (get-frontmatter file)
        {:keys [firn-tags roam-tags] } fm
        tags (or firn-tags roam-tags)
        tags (when tags (u/org-keyword->vector tags))]
    tags))

(defn get-keywords
  "Returns a list of org-keywords from a file. All files must have keywords."
  [f]
  (let [expected-keywords (get-in f [:as-edn :children 0 :children])]
    (when (= "keyword" (:type (first expected-keywords)))
      expected-keywords)))

(defn get-keyword
  "Fetches a(n org) #+keyword from a file, if it exists."
  [f keywrd]
  (->> f get-keywords (u/find-first #(= keywrd (:key %))) :value))

(defn get-link-parts
  "Converts `file:my_link.org` -> data of it's representative parts.
  file:my_link.org -> {:anchor nil :slug 'my_link'} "
  [org-link]
  (let [regex       #"(file:)(.*)\.(org)(\:\:\*.+)?"
        res          (re-matches regex org-link)
        anchor-link  (last res)
        anchor-link  (when anchor-link (-> res last u/clean-anchor))
        file-slug   (nth res 2)]
    {:anchor anchor-link :slug file-slug}))


(defn parse-front-matter->map
  "Converts an org-file's keywords into a map, evaling values as necessary.
   [{:type keyword, :key TITLE, :value Firn, :post_blank 0}
    {:type keyword, :key DATE_CREATED, :value <2020-03-01--09-53>, :post_blank 0}]
                               Becomes 
   {:title Firn, :date-created <2020-03-01--09-53>, :status active, :firn-layout project}"
  [f]
  (let [kw                  (get-keywords f)
        lower-case-it       #(when % (s/lower-case %))
        dash-it             #(when % (s/replace % #"_" "-"))
        key->keyword        (fn [k] (-> k :key lower-case-it dash-it keyword))
        has-req-frontmatter (some #(= (:key %) "TITLE") kw)
        eval-it             (fn [kw]
                              (let [k (key->keyword kw) v (kw :value)]
                                (if (u/in? keywords-to-eval k)
                                  {k (sci/eval-string v)}
                                  {k v})))]

    (when-not has-req-frontmatter
      (u/print-err! :warning "File <<" (f :name) ">> does not have 'front-matter' and will not be processed."))
    (->> kw (map eval-it) (into {}))))


;; -- Queries ----
;; Functions that return a boolean based on the contents of org-file map.

(defn headline-exported?
  [v]
  (u/in? (get-headline-tags v) "noexport"))

(defn is-private?
  "Returns true if a file meets the conditions of being 'private' Assumes the
  files has been read into memory and parsed to edn. A file is private (read:
  excluded) when it is in a private folder marked in :ignored-dirs in
  config.edn, when there is no #+TITLE keyword, or when #+FIRN_PRIVATE is true."
  [config f]
  (let [{:keys [title firn-private]} (-> f :meta :keywords)
        file-path                    (-> f :path (s/split #"/"))
        in-priv-folder?              (some (set file-path) (-> config :user-config :ignored-dirs))]
    (or (some? in-priv-folder?)
        (nil? title)
        firn-private)))

(defn in-site-map?
  "Checks if the processed file is in the site-map
  By default, all files are in the sitemap, so we check that the keywords is nil."
  [processed-file]
  (nil? (-> processed-file :meta :keywords :firn-sitemap?)))


;; -- Creators ----
;; Functions that construct strings, data from other data.

(defn make-headline-anchor
  "Takes a headline data structure and returns the id 'anchored' for slugifying"
  [node]
  (-> node get-headline-helper u/clean-anchor))

(defn make-file-tags
  "A file tag includes must-have-metadata attached on create"
  [{:keys [file-tags date-created-ts file-metadata]}]
  (let [file-tags (when file-tags (u/org-keyword->vector file-tags))]
    (when (seq file-tags)
      (map #(merge file-metadata {:tag-value % :date-created-ts date-created-ts}) file-tags))))

(defn make-site-map-item
  "When processing a file, we generate a site-map item that receives the pertinent
  metadata, and discards anything not needed."
  [processed-file site-url]
  (merge
   (dissoc (processed-file :meta) :logbook :links :toc :keywords :tags :attachments)
   {:path (str site-url "/" (processed-file :path-web))}))

;; -- Dates / Time


(defn sum-logbook
  "Iterates over a logbook and parses logbook :duration's and sums 'em up"
  [logbook]
  (let [hours-minutes [0 0]
        ;; Reduce ain't pretty. Should clean this up someday.
        reduce-fn     (fn [[acc-hours acc-minutes] log-entry]
                        (let [[hour min] (u/timestr->hours-min (:duration log-entry))
                              new-res    [(+ acc-hours hour) (+ acc-minutes min)]]
                          new-res))]
    (->> logbook
       (reduce reduce-fn hours-minutes)
       (u/timevec->time-str))))

(defn- sort-logbook
  "Loops over all logbooks, adds start and end unix timestamps."
  [logbook file-name]
  (let [mf #(parsed-org-date->unix-time %1 file-name)]
    (->> logbook
       ;; Filter out timestamps if they don't have a start or end.
       (filter #(and (% :start) (% :end) (% :duration)))
       ;; adds a unix timestamp for the :start and :end time so that's sortable.
       (map #(assoc % :start-ts (mf (:start %)) :end-ts (mf (:end %))))
       (sort-by :start-ts #(> %1 %2)))))

(defn extract-metadata-helper
  "There are lots of things we want to extract when iterating over the AST.
  Rather than filter/loop/map over it several times, it all happens here.
  Collects:
  - Logbooks
  - Links
  - Headings for TOC.
  - eventually... a plugin for custom file collection?"
  [tree-data file-metadata]
  (loop [tree-data     tree-data
         out           {:logbook            []
                        :logbook-total      nil
                        :links              []
                        :tags               []
                        :toc                []
                        :attachments        []
                        :__last-no-export__ nil}
         last-headline nil]  ; the most recent headline we've encountered.
    (if (empty? tree-data)

      ;; << The final formatting pre-return >>
      (let [out (update out :logbook #(sort-logbook % (file-metadata :from-file)))
            out (assoc out :logbook-total (sum-logbook (out :logbook)))]
        (dissoc out :__last-no-export__))

      ;; << The Loop >>
      (let [x  (first tree-data)
            xs (rest tree-data)]
        (case (:type x)
          "headline" ; if headline, collect data, push into toc, and set as "last-headline"
          (let [lvl      (x :level)
                last-ex  (out :__last-no-export__)
                toc-item {:level  lvl
                          :text   (get-headline-helper x)
                          :anchor (make-headline-anchor x)}
                ;; only add an item to the toc IF it is not a node or child of a headline tagged with :noexport:
                out      (if (headline-exported? x) ;;
                            (assoc out :__last-no-export__ x)
                            (if (and (not (nil? last-ex)) (> lvl (get last-ex :level)))
                              out
                              (-> out
                                  (update :toc conj toc-item)
                                  (assoc :__last-no-export__ nil))))]

            (recur xs out x))

          "title" ; if title, collect tags, map with metadata, push into out :tags
          (let [headline-link  (str (file-metadata :from-url)
                                    (make-headline-anchor last-headline))
                headline-meta  {:from-headline (get-headline-helper last-headline)
                                :headline-link headline-link}
                tags           (filter #(not= % "noexport") (x :tags)) ;; remove "noexport", all other tags are eligible.
                tags-with-meta (map #(merge headline-meta file-metadata {:tag-value %}) tags)
                add-tags       #(vec (concat % tags-with-meta))
                out            (update out :tags add-tags)]
            (recur xs out last-headline))

          "clock" ; if clock, merge headline-data into it, and push/recurse into out
          (let [headline-meta {:from-headline (-> last-headline :children first :raw)}
                new-log-item  (merge headline-meta file-metadata x)
                out           (update out :logbook conj new-log-item)]
            (recur xs out last-headline))

          "link" ; if link, also merge file metadata and push into new-links and recurse.
          (let [link-item (merge x file-metadata)
                out       (update out :links conj link-item)
                ;; if link starts with `file:` an ends with .png|.jpg|etc
                out       (if (u/is-attachment? (link-item :path))
                            (update out :attachments conj (link-item :path))
                            out)]
            (recur xs out last-headline))

          ;; default case, recur.
          (recur xs out last-headline))))))

(defn get-metadata
  "Iterates over a tree, and returns metadata for site-wide usage such as
  links (for graphing between documents, tbd) and logbook entries.
  Many of the values in the map are also contained in `keywords`"
  [file]
  (let [org-tree       (file :as-edn)
        tree-data      (tree-seq map? :children org-tree)
        keywords       (parse-front-matter->map file) ; keywords are "in-buffer-settings" - things that start with #+<MY_KEYWORD>
        {:keys [date-updated date-created title firn-under firn-order firn-tags roam-tags]} keywords
        file-tags      (or firn-tags roam-tags)
        file-metadata  {:from-file title
                        :from-url  (file :path-url)
                        :file-tags (when file-tags (u/org-keyword->vector file-tags))}
        metadata       (extract-metadata-helper tree-data file-metadata)
        date-parser    #(try
                          (when % (u/org-date->ts %))
                          (catch Exception e
                            (u/print-err! :error  (str "Could not parse date for file: " (or title "<unknown file>") "\nPlease confirm that you have correctly set the #+DATE_CREATED: and #+DATE_UPDATED values in your org file."))))
        file-tags      (make-file-tags {:file-tags       file-tags
                                         :file-metadata   file-metadata
                                         :date-created-ts (date-parser date-created)})]

    (merge metadata
           {:keywords        keywords
            :title           title
            :firn-under      (when firn-under (u/org-keyword->vector firn-under))
            :firn-order      firn-order
            :firn-tags       file-tags
            :date-updated    (when date-updated (u/strip-org-date date-updated))
            :date-created    (when date-created (u/strip-org-date date-created))
            :date-updated-ts (date-parser date-updated)
            :date-created-ts (date-parser date-created)})))

(defn make-file
  [config io-file]
  (let [name     (u/get-file-io-name io-file)
        path-abs (.getPath ^java.io.File io-file)
        path-web (u/get-web-path (config :dirname-files) path-abs)
        path-url (str (get-in config [:user-config :site-url]) "/"  path-web)
        as-json  (->> io-file slurp parse!)                     ; slurp the contents of a file and parse it to json.
        as-edn   (-> as-json (json/parse-string true))          ; convert the json to edn.
        ;; attach parsed data into a new file:
        new-file (assoc (empty-file) :name name :path path-abs :path-url path-web :path-web path-web :path-url path-url :as-json as-json :as-edn as-edn)
        new-file (assoc new-file :meta (get-metadata new-file)) ;; attach metadata
        ;; new-file (if render-html?)
        ]
    new-file))
