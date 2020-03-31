(ns firn.file
  "Functions for operating on an org file, read into memory."
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
  (-> f .getName (s/split #"\.") (first)))

(defn make
  "Creates a file; which is to say, a map of data & metadata about an org-file."
  [config io-file]
  (let [name     (get-io-name io-file)
        path-abs (-> io-file .getPath)
        path-web (get-web-path (config :dirname-files) path-abs)]
    {:as-edn    nil      ; JSON of org file -> converted to a map.
     :as-html   nil      ; the html output
     :as-json   nil      ; The org file, read as json and spat out by the rust binary.
     :keywords  nil      ; list of keywords at top of org file: #+TITLE:, #+CATEGORY, etc.
     :name      name     ; the file name
     :path      path-abs ; dir path to the file.
     :path-web  path-web ; path to file from cwd.
     :org-title nil      ; the #+TITLE value.
     :original  nil})) ; the file as as javaFile object.

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

(defn is-private?
  "Returns true if a file meets the conditions of being 'private'
  Assumes the files has been read into memory and parsed to edn."
  [config f]
  (let [is-private?     (get-keyword f "FIRN_PRIVATE")
        file-path       (f :path)
        in-priv-folder? (some (set file-path) (config :ignored-dirs))]
    (or
     (some? in-priv-folder?)
     (some? is-private?))))

(defn htmlify
  "Renders files according to their `layout` keyword."
  [config f]
  (println "CONFIG SITEMAP IS " (config :site-map))
  (let [layout   (keyword (get-keyword f "FIRN_LAYOUT"))
        as-html  (when-not (is-private? config f)
                   (layout/apply-layout config f layout))]
    (change f {:as-html as-html})))
