(ns firn.config
  (:require [clojure.string :as s]
            [firn.util :as u]))

(def curr-file
  {:name     nil
   :original nil         ; the file as as javaFile object.
   :org-title nil        ; the #+TITLE value.
   :keywords nil         ; list of keywords at top of org file: #+TITLE:, #+CATEGORY, etc.
   :as-json  nil         ; The org file, read as json and spat out by the rust binary.
   :as-edn   nil         ; JSON of org file -> converted to a map.
   :as-html  nil})       ; the html output

(def starting-config
  {
   :out-dirname   "_firn/_site"
   :out-dirpath   ""       ; cstrcted when default-config is made
   :ignored-dirs  ["priv"]
   :media-dir     "assets" ; org attachments to get copied into _site.
   :layouts       {}       ; layouts loaded into memory
   :layouts-dir   ""       ; where layouts are stored.
   :partials-dir  ""       ; where partials are stored.
   :files-dir     nil      ; where org content lives.
   :files-dirname nil
   :org-files     nil      ; a list of org files, added to as files get converted.
   :curr-file     curr-file})

(defn update-curr-file
  "Merges new values into the :curr-file map"
  [config new-m]
  (let [new-curr-file (merge (config :curr-file) new-m)]
    (assoc config :curr-file new-curr-file)))

(defn set-curr-file-original
  "Takes app-wide config and sets the current file being read on :curr-file"
  [config file]
  (update-curr-file config {:original file}))


;; -- "Getter" for pulling values out of the config -----------------------------


(defn get-curr-file-name
  [config]
  (-> config :curr-file :name))

(defn get-keywords
  "Gets keywords (ex: #+TITLE:) for a file."
  [config]
  (get-in config [:curr-file :as-edn :children 0 :children]))

(defn get-keyword
  [config keywrd]
  (->> config
       get-keywords
       (u/find-first #(= keywrd (:key %)))
       :value))

(defn file-is-private?
  "Returns true if a file meets the conditions of being 'private'
  Assumes the files has been read into memory and parsed to edn."
  [config]
  (let [is-private?     (get-keyword config "PRIVATE")
        file-path       (-> config :curr-file :original .getPath (s/split #"/"))
        in-priv-folder? (some (set file-path) (config :ignored-dirs))]
    (or
     (some? in-priv-folder?)
     (some? is-private?))))

(defn get-curr-file-keyword
  [config]
  (-> config
      :curr-file
      :keywords))

;; -- Default Config -----------------------------------------------------------

(defn default
  "Assume that files-dir does NOT end in a `/`
   ex: /Users/tees/Dropbox/wiki"
  [files-dir]
  (merge starting-config
         {:layouts-dir   (str files-dir "/_firn/layouts/")
          :partials-dir  (str files-dir "/_firn/partials/")
          :media-dir     (str files-dir "/" (starting-config :media-dir))
          :out-media-dir (str files-dir "/_firn/_site/" (starting-config :media-dir))
          :out-dirpath   (str files-dir "/" (starting-config :out-dirname))
          :files-dir     files-dir
          :files-dirname (-> files-dir (s/split #"/") last)}))
