(ns firn.build
  "Provides functions to core, to be called in the cli.
  Mostly to do with the processing of files / new site."
  (:require [clojure.java.io :as io]
            [firn.config :as config]
            [firn.file :as file]
            [firn.util :as u]
            [me.raynes.fs :as fs]))

(set! *warn-on-reflection* true)

(def default-files
  ["layouts/default.clj"
   "partials/head.clj"
   "config.edn"
   "static/css/main.css"])

(defn new-site
  "Creates the folders needed for a new site in your wiki directory.
  Copies the _firn_starter from resources, into where you are running the cmd."
  [{:keys [dir-files]}]
  (let [dir-files  (if (empty? dir-files) (u/get-cwd) dir-files)
        dir-firn   (config/make-dir-firn dir-files)
        base-dir   "firn/_firn_starter/"
        read-files (map #(hash-map :contents (slurp (io/resource (str base-dir %)))
                                   :out-name (str dir-firn "/" %)) default-files)]
    (if (fs/exists? dir-firn)
      (u/print-err! :error "A _firn directory already exists.")
      (do (fs/mkdir dir-firn)
          (doseq [f read-files]
            (io/make-parents (:out-name f))
            (spit (:out-name f) (:contents f)))))))

(defn setup
  "Creates folders for output, slurps in layouts and partials.

  NOTE: should slurp/mkdir/copy-dir be wrapped in try-catches? if-err handling?"
  [{:keys [dir-site
           dir-files
           dir-site-data
           dir-data
           dir-site-static
           dir-static] :as config}]
  (when-not (fs/exists? (config :dir-firn)) (new-site config))
  (fs/mkdir dir-site) ;; make _site

  ;; copy attachments and static files to final _site dir.
  (fs/delete-dir dir-site-data)
  (fs/copy-dir dir-data dir-site-data)

  (fs/delete-dir dir-site-static)
  (fs/copy-dir dir-static dir-site-static)

  (let [org-files (u/find-files-by-ext dir-files "org")
        layouts   (file/read-clj :layouts config)
        partials  (file/read-clj :partials config)]

    (assoc config :org-files org-files :layouts layouts :partials partials)))

(defn write-files
  "Takes a config, of which we can presume has :processed-files.
  Iterates on these files, and writes them to html using layouts."
  [config]
  (doseq [[_ f] (config :processed-files)]
    (let [out-file-name (str (config :dir-site) (f :path-web) ".html")]
      (when-not (file/is-private? config f)
        (io/make-parents out-file-name)
        (spit out-file-name (f :as-html)))))
  config)

(defn all-files
  "Processes all files in the org-directory"
  [{:keys [dir]}]
  (let [config (setup (config/prepare dir))
        {:keys [enable-rss?]} config]
    (cond->> config
      true        file/process-all
      enable-rss? file/write-rss-file!
      true        write-files)))
