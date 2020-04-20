(ns firn.build
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as s]
            [firn.config :as config]
            [firn.file :as file]
            [firn.util :as u]
            [me.raynes.fs :as fs])
  (:import [iceshelf.clojure.rust ClojureRust]))

(set! *warn-on-reflection* true)

(defn get-cwd
  "Because *fs/cwd* gives out the at-the-time jvm path.
  this works with graal."
  []
  (s/join "/" (-> (java.io.File. ".")
                  .getAbsolutePath
                  (s/split #"/")
                  drop-last)))

(defn prepare-config
  "TODO: docstring"
  [dir-files]
  (let [wiki-path (if (empty? dir-files) (get-cwd) dir-files)]
    (config/default wiki-path)))

(defn copy-site-template!
  "Takes the default site template and copies files to the dir-firn in confing
  NOTE: doing this because it's really frustrating / hard to figure out how
  to copy files from a compiled jar / native image from a resources directory."
  [dir-out]
  (let [base-dir        "firn/_firn_starter/"
        files           ["layouts/default.clj" "partials/head.clj" "partials/nav.clj" "static/css/bass.css" "static/css/main.css"]
        read-files      (map #(hash-map :contents (slurp (io/resource (str base-dir %)))
                                        :out-name (str dir-out "/" %)) files)]
    (doseq [f read-files]
      (io/make-parents (:out-name f))
      (spit (:out-name f) (:contents f)))))

(defn new-site
  "Creates the folders needed for a new site in your wiki directory.
  Copies the _firn_starter from resources, into where you are running the cmd."
  [{:keys [dir-files]}]
  (let [new-config (prepare-config dir-files)
        config     new-config
        dir-firn   (config :dir-firn)]
    (if (fs/exists? dir-firn)
      (println "A _firn directory already exists.")
      (do (fs/mkdir dir-firn)
          (copy-site-template! dir-firn)))))

(defn setup
  "Creates folders for output, slurps in layouts and partials.
  NOTE: should slurp/mkdir/copy-dir be wrapped in try-catches? if-err handling?"
  [{:keys [dir-layouts dir-partials dir-files] :as config}]
  (when-not (fs/exists? (config :dir-firn)) (new-site config))

  (let [layout-files  (u/find-files-by-ext dir-layouts "clj")
        partial-files (u/find-files-by-ext dir-partials "clj")
        partials-map  (u/file-list->key-file-map partial-files)
        org-files     (u/find-files-by-ext dir-files "org") ;; could bail if this is empty...
        layouts-map   (u/file-list->key-file-map layout-files)]

    (fs/mkdir (config :dirname-out)) ;; make _site

    ;; FIXME: These are not good - copying the entire attachment directory and the static folder.
    (when-not (fs/exists? (config :dir-site-attach))
      (fs/copy-dir (config :dir-attach) (config :dir-site-attach)))

    (when-not (fs/exists? (config :dir-site-static))
      (fs/copy-dir (config :dir-static) (config :dir-site-static)))

    (assoc
     config :org-files org-files :layouts layouts-map :partials partials-map)))

(defn parse!
  "Parse the org-mode file-string.
  NOTE: When developing with a REPL, this shells out to the rust bin.
  When compiled to a native image, it uses JNI to talk to the rust .dylib."
  [file-str]
  (if (u/native-image?)
    (ClojureRust/getFreeMemory file-str)
    (let [parser "../bin/parser"
            res    (sh/sh parser file-str)]
        (if-not (= (res :exit) 0)
          (prn "Orgize failed to parse file." file-str res)
          (res :out)))))

(defn process-file
  [config f]
  ;; munge the "file" datastructure; slowly filling it up, using let-shadowing.
  ;; At the end, we have a data structure that has converted:
  ;; org-mode file string -> json, edn, logbook, keywords
  ;; TODO: this could move to the file ns.
  (let [new-file      (file/make config f)
        as-json       (->> f slurp parse!)
        as-edn        (-> as-json (json/parse-string true))
        new-file      (file/change new-file {:as-json as-json :as-edn as-edn})
        file-metadata (file/extract-metadata new-file)
        new-file      (file/change new-file {:keywords  (file/get-keywords new-file)
                                             :org-title (file/get-keyword new-file "TITLE")
                                             :links     (file-metadata :links)
                                             :logbook   (file-metadata :logbook)})]
    new-file))

(defn write-files
  "Takes a config, of which we can presume has :processed-files.
  Iterates on these files, and writes them to html using layouts."
  [config]
  (doseq [f (config :processed-files)]
    (let [out-file-name (str (config :dir-site) (f :path-web) ".html")
          out-file      (file/htmlify config f)
          out-html      (out-file :as-html)]
      (when-not (file/is-private? config f)
        (io/make-parents out-file-name)
        (spit out-file-name out-html)))))

(defn process-files
  "Receives config, processes all files and builds up site-data
  logbooks, site-map, link-map, etc.
  This could be recursive, but am using atoms as it could
  be refactored in the future to be async and to use threads."
  [config]
  (let [site-links (atom [])
        site-logs  (atom [])
        site-map   (atom [])]
    (loop [org-files (config :org-files)
           output    []]
      (if (empty? org-files)
        (assoc config
               :processed-files output
               :site-map        @site-map
               :site-links      @site-links
               :site-logs       @site-logs)

        (let [next-file      (first org-files)
              processed-file (process-file config next-file)
              org-files      (rest org-files)
              output         (conj output processed-file)
              keyword-map    (file/keywords->map processed-file)
              new-site-map   (merge keyword-map {:path (processed-file :path-web)})
              file-metadata  (file/extract-metadata processed-file)]
          ;; add to sitemap.
          (when-not (file/is-private? config processed-file)
            (swap! site-map conj new-site-map)
            (swap! site-links concat @site-links (:links file-metadata))
            (swap! site-logs concat @site-logs (:logbook file-metadata)))
          ;; add links and logs to site wide data.
          (recur org-files output))))))

(defn all-files
  "Processes all files in the org-directory"
  [{:keys [dir-files]}]
  (let [config (setup (prepare-config dir-files))]
    (->> config
         process-files
         write-files)))
