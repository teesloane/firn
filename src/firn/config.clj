(ns firn.config
  (:require [clojure.string :as s]
            [firn.util :as u]))

(def curr-file
  {:as-edn    nil   ; JSON of org file -> converted to a map.
   :as-html   nil   ; the html output
   :as-json   nil   ; The org file, read as json and spat out by the rust binary.
   :keywords  nil   ; list of keywords at top of org file: #+TITLE:, #+CATEGORY, etc.
   :name      nil   ; the file name
   :org-title nil   ; the #+TITLE value.
   :original  nil}) ; the file as as javaFile object.


(def starting-config
  {:curr-file     curr-file     ; the current file we are processing.
   :dir-attach    "attach"      ; org attachments to get copied into _site.
   :dir-files     nil           ; where org content lives.
   :dir-layouts   ""            ; where layouts are stored.
   :dir-partials  ""            ; where partials are stored.
   :dirname-files nil           ; the name of directory where firn is run.
   :dirname-out   "_firn/_site" ; the root dir of the compiled firn site.
   :ignored-dirs  ["priv"]      ; Directories to ignore org files in.
   :layouts       {}            ; layouts loaded into memory
   :org-files     nil})         ; a list of org files, fetched when running setup.


;; -- "Setters" For setting vlaues into the config.

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
  (let [is-private?     (get-keyword config "FIRN_PRIVATE")
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
  [dir-files]
  (merge starting-config
         {:dir-firn        (str dir-files "/_firn")
          :dir-layouts     (str dir-files "/_firn/layouts/")
          :dir-partials    (str dir-files "/_firn/partials/")
          :dir-static      (str dir-files "/_firn/static/")
          :dir-attach      (str dir-files "/" (starting-config :dir-attach))
          :dir-site-static (str dir-files "/_firn/_site/static/")
          :dir-site-attach (str dir-files "/_firn/_site/" (starting-config :dir-attach))
          :dir-files       dir-files
          :dirname-files   (-> dir-files (s/split #"/") last) ;; the name of the dir where files are.
          :parser-path     (str dir-files "/_firn/bin/parser")}))
