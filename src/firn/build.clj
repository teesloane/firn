(ns firn.build
  "Functions related to the building of a static site."
  (:require [cheshire.core :as json]
            [clojure.java.shell :as sh]
            [clojure.java.io :as io]
            [firn.config :as config]
            [firn.file :as file]
            [me.raynes.fs :as fs]
            [firn.util :as u])
  (:gen-class))

(defn prepare-config
  "Takes a path to files (or CWD) and makes a config with it."
  [{:keys [path]}]
  (let [path   (if (empty? path) (.getPath fs/*cwd*) path)
        config (config/default path)]
    config))

(defn new-site
  "Creates the folders needed for a new site in your wiki directory.
  Copies the _firn_starter from resources, into where you are running the cmd.
  FIXME: This does not work with JARs - it's complicated to copy entire directories from a jar.
  possible solution: https://stackoverflow.com/a/28682910"
  [cmds & args]
  (let [new-config      (-> cmds prepare-config)
        existing-config (first args)
        config          (if (nil? cmds) existing-config new-config)]
     (if (fs/exists? (config :dir-firn))
       (u/print-err! "A _firn directory already exists.")
       (do
         (fs/copy-dir (io/resource "_firn_starter") (config :dir-firn))
         ;; used to be doing the following, when just copying the parser and
         ;; manually mkdirs... might have to revert to this:
         ;; b9259f7 * origin/feat/improve-templating Fix: vendor parser + move it to _firn/bin in setup
         ;; (-> "parser/bin/parser" io/resource io/input-stream (io/copy parser-out-path))))
         (fs/chmod "+x" (config :parser-path))))))

(defn setup
  "Creates folders for output, slurps in layouts and partials.
  NOTE: should slurp/mkdir/copy-dir be wrapped in try-catches? if-err handling?"
  [{:keys [dir-layouts dir-partials dir-files] :as config}]
  (when-not (fs/exists? (config :dir-firn)) (new-site nil config))

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
  "Shells out to the rust binary to parse the org-mode file."
  [config file-str]
  (let [parser (config :parser-path)
        res    (sh/sh parser file-str)]
    (if-not (= (res :exit) 0)
      (prn "Orgize failed to parse file." file-str res)
      (res :out))))

(defn process-file
  [config f]
  ;; munge the file: slowly filling it up, using let-shadowing, with data and metadata
  (let [new-file      (file/make config f)
        as-json       (->> f slurp (parse! config))
        as-edn        (-> as-json (json/parse-string true))
        new-file      (file/change new-file {:as-json as-json :as-edn as-edn})
        file-metadata (file/extract-metadata new-file)
        new-file      (file/change new-file {:keywords  (file/get-keywords new-file)
                                             :org-title (file/get-keyword new-file "TITLE")
                                             :links     (file-metadata :links)
                                             :logbook   (file-metadata :logbook)})]
    new-file))

(defn write-files
  "Takes a config, of which we can presume as :processed-files.
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
  be refactored in the future to be async and to use atoms."
  [config]
  (let [
        site-links (atom [])
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
  [opts]
  (let [config      (-> opts prepare-config setup)]
    (->> config
       process-files
       write-files)))
