(ns firn.core
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as s]
            [firn.config :as config]
            [firn.layout :as layout]
            [firn.markup :as m]
            [firn.util :as u]
            [hiccup.core :as h]
            [me.raynes.fs :as fs])
  (:gen-class))

(defn setup
  "Creates output directory for files and sets up starting
  config file that gets passed through all functions
  Also, moves your `media folder` into _site. TODO - make configurable..."
  [{:keys [files-dir out-dir media-dir] :as config}]

  (println "Running setup...")
  (println "Setup: Making _site output.")
  (fs/mkdir out-dir)

  (println "Copying root media into out media")
  (fs/copy-dir (config :media-dir) (config :out-media-dir))

  config)

(defn parse!
  "Shells out to the rust binary to parse the org-mode file."
  [file-str]
  (let [res (sh/sh "./src/parser/target/debug/parser" file-str)]
    (if-not (= (res :exit) 0)
      (prn "Failed to parse file.")
      (res :out))))

(defn get-files
  "Returns a list of files as Java objects. Filters out all non `.org` files."
  [{:keys [files-dir] :as config}]
  (println "Getting org files...")
  (let [site-dir       (io/file files-dir)
        site-dir-files (file-seq site-dir)
        org-files      (u/get-files-of-type site-dir-files ".org")]
    (cond
      (not (.isDirectory site-dir))
      (u/exit-with-err "No folder found at site-directory:" site-dir)

      (= (count org-files) 0)
      (u/exit-with-err "No .org files found in site-directory")

      :else
      (conj config {:org-files org-files}))))

(defn read-file
  "Pulls :curr-file from config > parses > put into config with new vals"
  [config]
  (let [file-orig   (-> config :curr-file :original)
        file-parsed (->> file-orig slurp parse!)
        file-name   (-> file-orig .getName (s/split #"\.") (first))]
    (config/update-curr-file config {:name file-name :as-json file-parsed})))

(defn dataify-file
  "Converts an org file into a bunch of data.
  TODO collect logbook, clock, for every file."
  [config]
  (let [file-json            (-> config :curr-file :as-json)
        file-edn             (-> file-json (json/parse-string true))
        file-keywords        (get-in file-edn [:children 0 :children])]
    (config/update-curr-file config {:as-edn file-edn :keywords file-keywords})))

(defn htmlify-file
  "Renders files according to their `layout` keyword."
  [config]
  (let [layout   (config/get-layout config)
        as-html  (layout/apply-template config layout)]

    (config/update-curr-file config {:as-html as-html})))

(defn write-file
  "Takes (file-)config input and writes html to output."
  [{:keys [out-dir curr-file]}]
  (let [curr-file-name (curr-file :name)
        out-file-name  (str out-dir curr-file-name ".html")
        out-html       (curr-file :as-html)]
    (println "Writing file: " curr-file-name)
    (spit out-file-name out-html)))

(defn -main
  [& args]
  (let [files-dir  (first args)
        config     (config/default files-dir)
        org-files  (-> config get-files :org-files)]

    (setup config) ;; side effectful
    (doseq [f org-files]
      (->> f
           (config/set-curr-file config)
           (read-file)
           (dataify-file)
           (htmlify-file)
           (write-file)))
    (if config/dev?
      (System/exit 0))))

;; (-main) ; I recommend not running this in your repl with many files. See test suite instead.
