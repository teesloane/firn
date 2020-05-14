(ns firn.file
  "Functions for operating and transforming on an org file, read into memory.
  The term  `file` here, generally refers to a _data structure_ and not a java io file.
  If the input is is a java io file, it should be called `file-io`

  You can view the file data-structure as it is made by the `make` function."
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [firn.layout :as layout]
            [firn.org :as org]
            [firn.util :as u]))

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
     :path-web  path-web ; path to file from cwd.
     :org-title nil      ; the #+TITLE value.
     :original  nil}))   ; the file as as javaFile object.

(defn change
  "Merges new keys into a file map."
  [f m]
  (merge f m))

;; FIXME: this gets called A LOT, it seems. Might want to profile.
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
  "Converts an org-file's keywords into a map.
   [{:type keyword, :key TITLE, :value Firn, :post_blank 0}
    {:type keyword, :key DATE_CREATED, :value 2020-03-01--09-53, :post_blank 0}]
                               Becomes 
   {:title Firn, :date-created 2020-03-01--09-53, :status active, :firn-layout project}"
  [f]
  (let [kw            (get-keywords f)
        lower-case-it #(when % (s/lower-case %))
        dash-it       #(when % (s/replace % #"_" "-"))
        key->keyword  (fn [k] (-> k :key lower-case-it dash-it keyword))]
    (into {} (map #(hash-map (key->keyword %) (% :value)) kw))))

(defn is-private?
  "Returns true if a file meets the conditions of being 'private'
  Assumes the files has been read into memory and parsed to edn."
  [config f]
  (let [is-private?     (get-keyword f "FIRN_PRIVATE")
        file-path       (-> f :path (s/split #"/"))
        in-priv-folder? (some (set file-path) (config :ignored-dirs))]
    (or
     (some? in-priv-folder?)
     (some? is-private?))))

;; TODO test me.
(defn sum-logbook
  "iterates over a logbook and parses string log sums and sums 'em up"
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
  (->> logbook
      ;; adds a unix timestamp for the :start and :end time.
       (map #(assoc % :start-ts (org/parsed-org-date->unix-time (:start %) file)
                    :end-ts   (org/parsed-org-date->unix-time (:end %) file)))
       (sort-by :start-ts #(> %1 %2))))

(defn extract-metadata-logbook-helper
  "Extracts the logbook and associates the parent headline's metadata with it.
  Because we are dealing with a flattened tree sequence we have to loop through
  items to keep track of headline values that precede a logbook.
  This is easier and more performant than searching an entire edn-tree of
  headings to see if they have a logbook to associate with.  ┬──┬◡ﾉ(° -°ﾉ)"
  [tree-seq]
  (loop [tree-items    tree-seq
         output        []
         last-headline nil]
    (if (empty? tree-items)
      output
      ;; if the item is a heading, store it for the next loop item to possibly access.
      (let [curr-item          (first tree-items)
            headline-val       (if (= (:type curr-item) "headline") curr-item last-headline)
            remaining-items    (rest tree-items)
            headline-meta-data {:from-headline (-> headline-val :children first :raw)} ;; raw headline for now.
            logbook-aug        #(merge % headline-meta-data)
            new-output         (if (= (:type curr-item) "clock")
                                 (conj output (logbook-aug curr-item))
                                 output)]
        (recur remaining-items new-output headline-val)))))

(defn extract-metadata
  "Iterates over a tree, and returns metadata for site-wide usage such as
  links (for graphing between documents, tbd) and logbook entries."
  [file]
  (let [org-tree       (file :as-edn)
        tree-data      (tree-seq map? :children org-tree)
        file-metadata  {:from-file (file :name) :from-file-path (file :path-web)}
        links          (filter #(= "link"  (:type %)) tree-data)
        logbook        (extract-metadata-logbook-helper tree-data)
        logbook-aug    (map #(merge % file-metadata) logbook)
        logbook-sorted (sort-logbook logbook-aug file)
        links-aug      (map #(merge % file-metadata) links)]
    {:links         links-aug
     :logbook       logbook-sorted
     :logbook-total (sum-logbook logbook) ;nil ;; TODO: this is going to be fun to calculate. Need to parse times.
     :keywords      (get-keywords file)
     :org-title     (get-keyword file "TITLE") ;; FIXME - dedupe this, remove keyword
     :title         (get-keyword file "TITLE")
     :firn-under    (get-keyword file "FIRN_UNDER")
     :date-updated  (get-keyword file "DATE_UPDATED")
     :date-created  (get-keyword file "DATE_CREATED")}))
    

(defn htmlify
  "Renders files according to their `layout` keyword."
  [config f]
  (let [layout   (keyword (get-keyword f "FIRN_LAYOUT"))
        as-html  (when-not (is-private? config f)
                   (layout/apply-layout config f layout))]
    ;; as-html
    (change f {:as-html as-html})))

(defn process-one
  "Munge the 'file' datastructure; slowly filling it up, using let-shadowing.
  Essentially, converts `org-mode file string` -> json, edn, logbook, keywords"
  [config f]
  (let [new-file      (make config f)
        as-json       (->> f slurp org/parse!)
        as-edn        (-> as-json (json/parse-string true))
        new-file      (change new-file {:as-json as-json :as-edn as-edn})
        file-metadata (extract-metadata new-file)
        new-file      (change new-file {:meta file-metadata} #_{:keywords  (get-keywords new-file)
                                                                :org-title (get-keyword new-file "TITLE")
                                                                :meta      (file-metadata)
                                                                :links     (file-metadata :links)
                                                                :logbook   (file-metadata :logbook)})
        final-file    (htmlify config new-file)]

    final-file))

(defn process-all
  "Receives config, processes all files and builds up site-data
  logbooks, site-map, link-map, etc."
  [config]
  (let [site-links (atom [])
        site-logs  (atom [])
        site-map   (atom [])]
    ;; recurse over the org-files, gradually processing them and
    ;; pulling out links, logs, and other useful data.
    (loop [org-files (config :org-files)
           output    {}]
      (if (empty? org-files)
        ;; LOOP/RECUR: run one more loop on all files, and create their html,
        ;; now that we have processed everything.
        ;; FIXME bad formatting in assoc
        (let [config-with-data (assoc config :processed-files output :site-map @site-map
                                      :site-links @site-links :site-logs  @site-logs)
              with-html        (into {} (for [[k pf] output] [k (htmlify config-with-data pf)]))
              final            (assoc config-with-data :processed-files with-html)]
          final)

        (let [next-file      (first org-files)
              processed-file (process-one config next-file)
              org-files      (rest org-files)
              output         (assoc output (processed-file :path-web) processed-file)
              keyword-map    (keywords->map processed-file)
              new-site-map   (merge keyword-map {:path (processed-file :path-web)})]

          ;; add to sitemap when file is not private.
          (when-not (is-private? config processed-file)
            (swap! site-map conj new-site-map)
            (swap! site-links concat @site-links (-> processed-file :meta :links) #_(:links file-metadata)) ;; FIXME clean up
            (swap! site-logs concat @site-logs (-> processed-file :meta :links) #_(:logbook file-metadata))) ;; FIXME: cleanup
          ;; add links and logs to site wide data.
          (recur org-files output))))))

(defn reload-requested-file
  "Take a request to a file, pulls the file out of memory
  grabs the path of the original file, reslurps it and reprocesses"
  [file config]
  (let [re-slurped (-> file :path io/file)
        re-processed (process-one config re-slurped)]
    re-processed))
