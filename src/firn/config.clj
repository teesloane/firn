(ns firn.config
  (:require [clojure.string :as s]))

(def dev? true)

(def curr-file
  {:name     nil
   :original nil         ; the file as as javaFile object.
   :org-title nil        ; the #+TITLE value.
   :keywords nil         ; list of keywords at top of org file: #+TITLE:, #+CATEGORY, etc.
   :as-json  nil         ; The org file, read as json and spat out by the rust binary.
   :as-edn   nil         ; JSON of org file -> converted to a map.
   :as-html  nil})       ; the html output

(def starting-config
  {:out-dir       nil      ; where files get published. TODO - change this to "out-dirpath"
   :out-dirname   "_site"
   :media-dir     "assets" ; org attachments to get copied into _site.
   :layouts       {}       ; layouts loaded into memory
   :layouts-dir   ""       ; where layouts are stored.
   :partials-dir  ""       ; where partials are stored.
   :files-dir     nil      ; where org content lives.
   :files-dirname nil
   :org-files     nil      ; a list of org files, added to as files get converted.
   :curr-file     curr-file})

(defn set-curr-file
  "Takes app-wide config and sets the current file being read on :curr-file"
  [config file]
  (let [cf (assoc curr-file :original file)
        config (assoc config :curr-file cf)]
    config))

(defn update-curr-file
  [config new-m]
  (let [new-curr-file (merge (config :curr-file) new-m)]
    (assoc config :curr-file new-curr-file)))

(defn get-curr-file-name
  [config]
  (-> config :curr-file :name))

(defn get-layout
  "Pulls the `#+LAYOUT` value out of a current file.
  Returns nil if it doesn't exist.
  TODO - write test"
  [config]
  (->> config
       (:curr-file)
       (:keywords)
       (filter #(= (:key %) "LAYOUT"))
       (first)
       (:value)
       (keyword)))

(defn default
  [files-dir]
  (merge starting-config
         {:out-dir       (str files-dir (starting-config :out-dirname) "/")
          :layouts-dir   (str files-dir "_layouts/")
          :partials-dir  (str files-dir "_partials/")
          :out-media-dir (str files-dir "_site/" (starting-config :media-dir))
          :files-dir     files-dir
          :files-dirname (-> files-dir (s/split #"/") last)

          :media-dir (str files-dir "/" (starting-config :media-dir))}))
