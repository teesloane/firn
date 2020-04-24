(ns firn.build
  "Provides functions to core, to be called in the cli.
  Mostly to do with the processing of files / new site."
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [firn.config :as config]
            [firn.file :as file]
            [firn.util :as u]
            [firn.org :as org]
            [me.raynes.fs :as fs]))

(set! *warn-on-reflection* true)

;; TODO: remove custom templates for release.
(def default-files ["layouts/default.clj"
                    "layouts/project.clj"
                    "layouts/index.clj"
                    "partials/head.clj"
                    "partials/nav.clj"
                    "config.edn"
                    "static/css/bass.css"
                    "static/css/main.css"])


(defn new-site
  "Creates the folders needed for a new site in your wiki directory.
  Copies the _firn_starter from resources, into where you are running the cmd."
  [{:keys [dir-files]}]
  (let [dir-firn     (config/make-dir-firn dir-files)
        base-dir     "firn/_firn_starter/"
        read-files   (map #(hash-map :contents (slurp (io/resource (str base-dir %)))
                                     :out-name (str dir-firn "/" %)) default-files)]
    (if (fs/exists? dir-firn)
      (do
        (println "A _firn directory already exists.")
        false)
      (do (fs/mkdir dir-firn)
          (doseq [f read-files]
            (io/make-parents (:out-name f))
            (spit (:out-name f) (:contents f)))))))

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

    ;; copy attachments and static files too final _site dir.
    (when-not (fs/exists? (config :dir-site-attach))
      (fs/copy-dir (config :dir-attach) (config :dir-site-attach)))
    (when-not (fs/exists? (config :dir-site-static))
      (fs/copy-dir (config :dir-static) (config :dir-site-static)))

    (assoc
     config :org-files org-files :layouts layouts-map :partials partials-map)))

(defn process-file
  "munge the 'file' datastructure; slowly filling it up, using let-shadowing.
  At the end, we have a data structure that has converted: org-mode file string -> json, edn, logbook, keywords
  TODO: this could move to the file ns.
  "
  [config f]
  (let [new-file      (file/make config f)
        as-json       (->> f slurp org/parse!)
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
  logbooks, site-map, link-map, etc."
  [config]
  (let [site-links (atom [])
        site-logs  (atom [])
        site-map   (atom [])]
    ;; recurse over the org-files, gradually processing them and
    ;; pulling out links, logs, and other useful data.
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

          ;; add to sitemap when file is not private.
          (when-not (file/is-private? config processed-file)
            (swap! site-map conj new-site-map)
            (swap! site-links concat @site-links (:links file-metadata))
            (swap! site-logs concat @site-logs (:logbook file-metadata)))
          ;; add links and logs to site wide data.
          (recur org-files output))))))

(defn all-files
  "Processes all files in the org-directory"
  [{:keys [dir-files]}]
  (let [config (setup (config/prepare dir-files))]
    (->> config
         process-files
         write-files)))
