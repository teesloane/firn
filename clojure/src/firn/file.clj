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
(def keywords-to-eval [:firn-toc :firn-properties?])

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
  [dir {:keys [dir-partials dir-layouts]}]
  (case dir
    :layouts
    (-> dir-layouts (u/find-files-by-ext "clj") (u/load-fns-into-map))

    :partials
    (-> dir-partials (u/find-files-by-ext "clj") (u/load-fns-into-map))

    (throw (Exception. "Ensure you are passing the right possible keywords to read-clj."))))

(defn make
  "Creates a file; which is to say, a map of data & metadata about an org-file."
  [config io-file]
  (let [name     (get-io-name io-file)
        path-abs (.getPath ^java.io.File io-file) ; (-> io-file ^java.io.File .getPath)
        path-web (get-web-path (config :dirname-files) path-abs)]
    {:as-edn    nil      ; JSON of org file -> converted to a map.
     :as-html   nil      ; the html output
     :as-json   nil      ; The org file, read as json and spat out by the rust binary.
     :keywords  nil      ; list of keywords at top of org file: #+TITLE:, #+CATEGORY, etc.
     :name      name     ; the file name
     :path      path-abs ; dir path to the file.
     :meta      {}       ; is filled when process-file / extract-metadata is run.
     :path-web  path-web ; path to file from cwd.
     :original  nil}))   ; the file as as javaFile object.

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
  [logbook file]
  (let [mf #(org/parsed-org-date->unix-time %1 file)]
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
         out-logs      []
         out-links     []
         out-toc       []
         last-headline nil]  ; the most recent headline we've encountered.
    (if (empty? tree-data)
      ;; All done! return the collected stuff.
      {:logbook out-logs
       :toc     out-toc
       :links   out-links}
      ;; Do the work.
      (let [x  (first tree-data)
            xs (rest tree-data)]
        (case (:type x)
          "headline" ; if headline, collect data, push into toc, and set as "last-headline"
          (let [toc-item {:level (x :level)
                          :text (org/get-headline-helper x)
                          :anchor (org/make-headline-anchor x)}
                new-toc  (conj out-toc toc-item)]
            (recur xs out-logs out-links new-toc x))

          "clock" ; if clock, merge headline-data into it, and push/recurse new-logs.
          (let [headline-meta {:from-headline (-> last-headline :children first :raw)}
                log-augmented (merge headline-meta file-metadata x)
                new-logs      (conj out-logs log-augmented)]
            (recur xs new-logs out-links out-toc last-headline))

          "link" ; if link, also merge file metadata and push into new-links and recurse.
          (let [link-item (merge x file-metadata)
                new-links (conj out-links link-item)]
            (recur xs out-logs new-links out-toc last-headline))

          ;; default case, recur.
          (recur xs out-logs out-links out-toc last-headline))))))

(defn extract-metadata
  "Iterates over a tree, and returns metadata for site-wide usage such as
  links (for graphing between documents, tbd) and logbook entries."
  [file]
  (let [org-tree       (file :as-edn)
        tree-data      (tree-seq map? :children org-tree)
        file-metadata  {:from-file (file :name) :from-file-path (file :path-web)}
        date-updated   (get-keyword file "DATE_UPDATED")
        date-created   (get-keyword file "DATE_CREATED")
        metadata       (extract-metadata-helper tree-data file-metadata)
        logbook-sorted (sort-logbook (metadata :logbook) file)]

    {:links           (metadata :links)
     :logbook         logbook-sorted
     :logbook-total   (sum-logbook logbook-sorted)
     :keywords        (get-keywords file)
     :title           (get-keyword file "TITLE")
     :firn-under      (get-keyword file "FIRN_UNDER")
     :toc             (metadata :toc)
     :date-updated    (when date-updated (u/strip-org-date date-updated))
     :date-created    (when date-created (u/strip-org-date date-created))
     :date-updated-ts (when date-updated (u/org-date->ts date-updated))
     :date-created-ts (when date-created (u/org-date->ts date-created))}))
