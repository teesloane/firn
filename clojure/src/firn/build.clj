(ns firn.build
  "Provides functions to core, to be called in the cli.
  Mostly to do with the processing of files / new site."
  (:require [clojure.java.io :as io]
            [firn.config :as config]
            [firn.file :as file]
            [clj-rss.core :as rss]
            [firn.org :as org]
            [firn.layout :as layout]
            [cheshire.core :as json]
            [firn.util :as u]
            [me.raynes.fs :as fs]))

(set! *warn-on-reflection* true)

(def default-files
  ["layouts/default.clj"
   "partials/head.clj"
   "config.edn"
   "static/css/firn_base.css"])

(defn new-site
  "Creates the folders needed for a new site in your wiki directory.
  Copies the _firn_starter from resources, into where you are running the cmd."
  [{:keys [dir]}]
  (let [dir  (if (empty? dir) (u/get-cwd) dir)
        dir-firn   (config/make-dir-firn dir)
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

(defn htmlify
  "Renders files according to their `layout` keyword."
  [config f]
  (let [layout   (keyword (file/get-keyword f "FIRN_LAYOUT"))
        as-html  (when-not (file/is-private? config f)
                   (layout/apply-layout config f layout))]
    ;; as-html
    (file/change f {:as-html as-html})))

(defn process-one
  "Munge the 'file' datastructure; slowly filling it up, using let-shadowing.
  Essentially, converts `org-mode file string` -> json, edn, logbook, keywords"
  [config f]

  (let [new-file      (file/make config f)                                     ; make an empty "file" map.
        as-json       (->> f slurp org/parse!)                            ; slurp the contents of a file and parse it to json.
        as-edn        (-> as-json (json/parse-string true))               ; convert the json to edn.
        new-file      (file/change new-file {:as-json as-json :as-edn as-edn}) ; shadow the new-file to add the json and edn.
        file-metadata (file/extract-metadata new-file)                         ; collect the file-metadata from the edn tree.
        new-file      (file/change new-file {:meta file-metadata})             ; shadow the file and add the metadata
        ;; TODO PERF: htmlify happening as well in `process-all`.
        ;; this is due to the dev server. There should be a conditional
        ;; that checks if we are running in server.
        final-file    (htmlify config new-file)]                   ; parses the edn tree -> html.

    final-file))

(defn process-all
  "Receives config, processes all files and builds up site-data
  logbooks, site-map, link-map, etc."
  [config]
  (let [site-links (atom [])
        site-logs  (atom [])
        site-map   (atom [])]
    ;; recurse over the org-files, gradually processing them and
    ;; pulling out links, logs, and other useful data.
    (loop [org-files (config :org-files)
           output    {}]
      (if (empty? org-files)
        ;; LOOP/RECUR: run one more loop on all files, and create their html,
        ;; now that we have processed everything.
        (let [config-with-data (assoc config
                                      :processed-files output
                                      :site-map        @site-map
                                      :site-links      @site-links
                                      :site-logs       @site-logs)
              ;; FIXME: I think we are rendering html twice here, should prob only happen here?
              with-html        (into {} (for [[k pf] output] [k (htmlify config-with-data pf)]))
              final            (assoc config-with-data :processed-files with-html)]
          final)

        ;; Otherwise continue...
        (let [next-file         (first org-files)
              processed-file    (process-one config next-file)
              is-private        (file/is-private? config processed-file)
              org-files         (rest org-files)
              output            (if is-private
                                  output
                                  (assoc output (processed-file :path-web) processed-file))
              new-site-map-item (merge
                                 (dissoc (processed-file :meta) :logbook :links :keywords :toc)
                                 {:path (str "/" (processed-file :path-web))})]

          ;; add to sitemap when file is not private.
          (when-not is-private
            (swap! site-map conj new-site-map-item)
            (swap! site-links concat (-> processed-file :meta :links))
            (swap! site-logs concat  (-> processed-file :meta :logbook)))
          ;; add links and logs to site wide data.
          (recur org-files output))))))

(defn write-rss-file!
  "Build an rss file. It sorts files by file:meta:date-created, writes to feed.xml"
  [{:keys [processed-files dir-site user-config] :as config}]
  (println "Building rss file...")
  (let [{:keys [site-title site-url site-desc]} user-config
        feed-file   (str dir-site "feed.xml")
        first-entry {:title site-title :link site-url :description site-desc}
        make-rss    (fn [[_ f]]
                      (hash-map :title   (-> f :meta :title)
                                :link    (str site-url "/" (-> f :path-web))
                                :pubDate (u/org-date->java-date  (-> f :meta :date-created))
                                :description (str (f :as-html))))]
    (io/make-parents feed-file)
    (->> processed-files
       (filter (fn [[_ f]] (-> f :meta :date-created)))
       (map make-rss)
       (sort-by :pubDate)
       (reverse)
       (u/prepend-vec first-entry) ; first entry must be about the site
       (apply rss/channel-xml)
       (spit feed-file)))
  config)

(defn write-files
  "Takes a config, of which we can presume has :processed-files.
  Iterates on these files, and writes them to html using layouts. Must return
  the config for the defstate server to be able to store config in an atom."
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
        rss?   (-> config :user-config :enable-rss?)]
    (cond->> config
      true process-all
      rss? write-rss-file!
      true write-files)))

(defn reload-requested-file
  "Take a request to a file, pulls the file out of memory
  grabs the path of the original file, reslurps it and reprocesses"
  [file config]
  (let [re-slurped (-> file :path io/file)
        re-processed (process-one config re-slurped)]
    re-processed))

