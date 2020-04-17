(ns firn.file
  "Functions for operating and transforming on an org file, read into memory.
  The term  `file` here, generally refers to a _data structure_ and not a java io file.
  If the file is a java io file, it should be called `file-io`

  You can view the file data-structure as it is made by the `make` function.
  Some of the functions in this namespace are for operating on a io file, others are
  for operating on the file map of data."
  (:require [clojure.string :as s]
            [firn.util :as u]
            [firn.layout :as layout]))
           

(defn strip-file-ext
  "Removes a file extension from a file path string.
  (strip-file-ext foo/jo.jpeg jpeg) ;; => foo/jo"
  [ext string]
  (let [ext-regex (re-pattern (str "\\.(" ext ")$"))
        res (s/replace string ext-regex "")]
    res))

(defn get-web-path
  "Determines the web path of the file from the cwd.
  `dirname-files`: demo_org
  `file-path-abs`: /Users/tees/Projects/firn/firn/test/firn/demo_org/jam/jo/foo/file2.org
  `returns`      : jam/jo/foo/file2"
  [dirname-files file-path-abs]
  (let [path-abs-list (-> file-path-abs (s/split #"/"))]
    (loop [dirs-reversed (reverse path-abs-list)
           out           []]
      (if (= (first dirs-reversed) dirname-files)
        (->> out reverse (s/join "/") (strip-file-ext "org"))
        (recur (rest dirs-reversed)
               (conj out (first dirs-reversed)))))))

(defn get-io-name
  "Returns the name of a file from the Java ioFile object w/o an extension."
  [f]
  (let [f-name (.getName ^java.io.File f)]
    (-> f-name (s/split #"\.") (first))))

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

(defn get-keywords
  "Returns a list of org-keywords from a file.
  Presumes that all org files start with a keyword (NOTE: I think.)"
  [f]
  (get-in f [:as-edn :children 0 :children]))

(defn get-keyword
  "Fetches a(n org) #+keyword from a file, if it exists."
  [f keywrd]
  (->> f get-keywords (u/find-first #(= keywrd (:key %))) :value))

(defn keywords->map
  "Converts an org-file's keywords into a map.
   [{:type keyword, :key TITLE, :value Firn, :post_blank 0}
    {:type keyword, :key DATE_CREATED, :value 2020-03-01--09-53, :post_blank 0}]
                               Becomes 
   {:title Firn, :date-created 2020-03-01--09-53, :status active, :firn-layout project}
  "
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

(defn extract-metadata-logbook-helper
  "Extracts the logbook and associates the parent headline's metadata with it.
  Because we are dealing with a flattened tree sequence we have to loop through items to
  keep track of headline values that precede a logbook. This is easier and more performant
  than searching an entire edn-tree of headings to see if they have a logbook to associate with.
  ... I think. ┬──┬◡ﾉ(° -°ﾉ)"
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
  (let [org-tree      (file :as-edn)
        tree-data     (tree-seq map? :children org-tree)
        file-metadata {:from-file (file :name) :from-file-path (file :path-web)}
        links         (filter #(= "link"  (:type %)) tree-data)
        logbook       (extract-metadata-logbook-helper tree-data)
        logbook-aug   (map #(merge % file-metadata) logbook)
        links-aug     (map #(merge % file-metadata) links)]
    {:links links-aug :logbook logbook-aug}))

(defn htmlify
  "Renders files according to their `layout` keyword."
  [config f]
  (let [layout   (keyword (get-keyword f "FIRN_LAYOUT"))
        as-html  (when-not (is-private? config f)
                   (layout/apply-layout config f layout))]
    (change f {:as-html as-html})))
