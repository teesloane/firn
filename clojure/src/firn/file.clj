(ns firn.file
  "Functions for operating and transforming on an org file, read into memory.
  The term  `file` here, generally refers to a _data structure_ and not a java io file.
  If the input is is a java io file, it should be called `file-io`

  You can view the file data-structure as it is made by the `make` function."
  (:require [clojure.string :as s]
            [firn.org :as org]
            [firn.util :as u]
            [sci.core :as sci]))

;; #+ORG_KEYWORDS that we want to evaluate into real clojure values.
(def keywords-to-eval [:firn-toc :firn-properties? :firn-order :firn-fold :firn-sitemap?])

;; -- Getters

(defn get-web-path
  "Determines the web path of the file from the cwd.
  `dirname-files`: demo_org
  `file-path-abs`: /Users/tees/Projects/firn/firn/test/firn/demo_org/jam/jo/foo/file2.org
  `returns`      : jam/jo/foo/file2

  NOTE: currently, you cannot have the `name` of your folder of org
  files appear earlier in the path to those files.
  invalid example: `/users/foo/my-wiki/another-dir/my-wiki/file1.org`"

  [dirname-files file-path-abs]
  (if (u/dupe-name-in-dir-path? file-path-abs dirname-files)
    (u/print-err! :error "\nWell, well, well. You've stumbled into one of weird edge cases of using Firn. \nCongrats on getting here! Let's look at what's happening. \n\nThe directory of your org files appears twice in it's path:\n\n<<" file-path-abs ">>\n\nIn order to properly build web-paths for your files, Firn needs to know where your 'web-root' is. \nWe cannot currently detect which folder is your file root. \nTo solve this, either rename your directory of org files: \n\n<<" dirname-files ">>\n\nor rename the earlier instance in the path of the same name.")
    (->> (s/split file-path-abs #"/")
       (drop-while #(not (= % dirname-files)))
       rest
       (s/join "/")
       (u/remove-ext))))

(defn get-io-name
  "Returns the name of a file from the Java ioFile object w/o an extension."
  [f]
  (let [f-name (.getName ^java.io.File f)]
    (-> f-name (s/split #"\.") (first))))

(defn read-clj
  "Reads a folder full of clj files, such as partials or layouts.
  pass a symbol for dir to request a specific folder."
  [dir {:keys [dir-partials dir-layouts dir-pages]}]
  (case dir
    :layouts
    (-> dir-layouts (u/find-files-by-ext "clj") (u/load-fns-into-map))

    :partials
    (-> dir-partials (u/find-files-by-ext "clj") (u/load-fns-into-map))

    :pages
    (-> dir-pages (u/find-files-by-ext "clj") (u/load-fns-into-map))

    (throw (Exception. "Ensure you are passing the right possible keywords to read-clj."))))

(defn make
  "Creates a file; which is to say, a map of data & metadata about an org-file."
  [config io-file]
  (let [name     (get-io-name io-file)
        path-abs (.getPath ^java.io.File io-file)
        path-web (get-web-path (config :dirname-files) path-abs)
        path-url (str (get-in config [:user-config :site-url]) "/"  path-web)]

    {:as-edn   nil      ; JSON of org file -> converted to a map.
     :as-html  nil      ; the html output
     :as-json  nil      ; The org file, read as json and spat out by the rust binary.
     :keywords nil      ; list of keywords at top of org file: #+TITLE:, #+CATEGORY, etc.
     :name     name     ; the file name
     :path     path-abs ; dir path to the file.
     :meta     {}       ; is filled when process-file / extract-metadata is run.
     :path-web path-web ; path to file from cwd: some/dirs/to/the/file - not well named.
     :path-url path-url
     :original nil}))   ; the file as as javaFile object.

(defn change
  "Merges new keys into a file map."
  [f m]
  (merge f m))

(defn get-keywords
  "Returns a list of org-keywords from a file. All files must have keywords."
  [f]
  (let [expected-keywords (get-in f [:as-edn :children 0 :children])]
    (if (= "keyword" (:type (first expected-keywords)))
      expected-keywords
      (u/print-err! :error "The org file <<" (f :name) ">> does not have 'front-matter' Please set at least the #+TITLE keyword for your file."))))

(defn get-keyword
  "Fetches a(n org) #+keyword from a file, if it exists."
  [f keywrd]
  (->> f get-keywords (u/find-first #(= keywrd (:key %))) :value))

(defn keywords->map
  "Converts an org-file's keywords into a map, evaling values as necessary.
   [{:type keyword, :key TITLE, :value Firn, :post_blank 0}
    {:type keyword, :key DATE_CREATED, :value <2020-03-01--09-53>, :post_blank 0}]
                               Becomes 
   {:title Firn, :date-created <2020-03-01--09-53>, :status active, :firn-layout project}"
  [f]
  (let [kw               (get-keywords f)
        lower-case-it    #(when % (s/lower-case %))
        dash-it          #(when % (s/replace % #"_" "-"))
        key->keyword     (fn [k] (-> k :key lower-case-it dash-it keyword))
        eval-it          (fn [kw]
                           (let [k (key->keyword kw) v (kw :value)]
                             (if (u/in? keywords-to-eval k)
                               {k (sci/eval-string v)}
                               {k v})))]
    (->> kw (map eval-it) (into {}))))

(defn is-private?
  "Returns true if a file meets the conditions of being 'private'
  Assumes the files has been read into memory and parsed to edn."
  [config f]
  (let [is-private?     (get-keyword f "FIRN_PRIVATE")
        file-path       (-> f :path (s/split #"/"))
        in-priv-folder? (some (set file-path) (-> config :user-config :ignored-dirs))]
    (or
     (some? in-priv-folder?)
     (some? is-private?))))

(defn in-site-map?
  "Checks if the processed file is in the site-map
  By default, all files are in the sitemap, so we check that the keywords is nil."
  [processed-file]
  (nil? (-> processed-file :meta :keywords :firn-sitemap?)))

(defn make-site-map-item
  "When processing a file, we generate a site-map item that receives the pertinent
  metadata, and discards anything not needed."
  [processed-file site-url]
  (merge
   (dissoc (processed-file :meta) :logbook :links :toc :keywords :tags :attachments)
   {:path (str site-url "/" (processed-file :path-web))}))

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
  (let [mf #(org/parsed-org-date->unix-time %1 file-name)]
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
                          :text   (org/get-headline-helper x)
                          :anchor (org/make-headline-anchor x)}
                ;; only add an item to the toc IF it is not a node or child of a headline tagged with :noexport:
                out      (if (org/headline-exported? x) ;;
                            (assoc out :__last-no-export__ x)
                            (if (and (not (nil? last-ex)) (> lvl (get last-ex :level)))
                              out     
                              (-> out
                                  (update :toc conj toc-item)
                                  (assoc :__last-no-export__ nil))))]

            (recur xs out x))

          "title" ; if title, collect tags, map with metadata, push into out :tags
          (let [headline-link  (str (file-metadata :from-url)
                                    (org/make-headline-anchor last-headline))
                headline-meta  {:from-headline (org/get-headline-helper last-headline)
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

(defn craft-file-tags
  "A file tag includes must-have-metadata attached on create"
  [{:keys [file-tags date-created-ts file-metadata]}]
  (let [file-tags (when file-tags (u/org-keyword->vector file-tags))]
    (when (seq file-tags)
      (map #(merge file-metadata {:tag-value % :date-created-ts date-created-ts}) file-tags))))

(defn extract-metadata
  "Iterates over a tree, and returns metadata for site-wide usage such as
  links (for graphing between documents, tbd) and logbook entries.
  Many of the values in the map are also contained in `keywords`"
  [file]
  (let [org-tree       (file :as-edn)
        tree-data      (tree-seq map? :children org-tree)
        keywords       (keywords->map file) ; keywords are "in-buffer-settings" - things that start with #+<MY_KEYWORD>
        {:keys [date-updated date-created title firn-under firn-order firn-tags roam-tags]} keywords
        file-tags      (or firn-tags roam-tags)
        file-metadata  {:from-file title
                        :from-url  (file :path-url)
                        :file-tags (when file-tags (u/org-keyword->vector file-tags))}
        metadata       (extract-metadata-helper tree-data file-metadata)
        date-parser    #(try
                          (when % (u/org-date->ts date-created))
                          (catch Exception e
                            (u/print-err! :error  (str "Could not parse date for file: " (or title "<unknown file>") "\nPlease confirm that you have correctly set the #+DATE_CREATED: and #+DATE_UPDATED values in your org file."))))
        file-tags      (craft-file-tags {:file-tags       file-tags
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
