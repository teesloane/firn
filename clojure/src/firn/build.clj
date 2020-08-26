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
            [me.raynes.fs :as fs]
            [hiccup.core :as h]
            [clojure.string :as str]))

(set! *warn-on-reflection* true)

(def default-files
  ["layouts/default.clj"
   "pages/tags.clj"
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
        pages     (file/read-clj :pages config)
        partials  (file/read-clj :partials config)]
    (assoc config :org-files org-files :layouts layouts :partials partials :pages pages)))

(defn htmlify
  "Renders files according to their `layout` keyword."
  [config f]
  (let [layout   (keyword (file/get-keyword f "FIRN_LAYOUT"))
        as-html  (when-not (file/is-private? config f)
                   (layout/apply-layout config f layout))]
    ;; as-html
    (file/change f {:as-html as-html})))

(defn make-site-map
  "Builds the site maps a tree where a file might fall under one or more files.
  Checks the value of `#+FIRN_UNDER` to decide under what parent to place a child."
  [processed-files]
  (loop [files processed-files
         out   {}]
    (if (seq files)
      (let [head       (first files)
            tail       (rest files)
            firn-under (-> head :firn-under)
            title      (-> head :title)]
        (cond
          ;; if there is no "firn-under" it's a top level site-map item, and
          ;; it's not in the map yet.
          (and (nil? firn-under) (not (contains? out title)))
          (recur tail (assoc out title head))

          ;; the item is already in `out` b/c the firn-under val created it with
          ;; an update-in call.
          (contains? out title)
          (let [updated-out (update out title #(merge % head))]
            (recur tail updated-out))

          ;; if user wants to make a nested value, do that.
          (seq firn-under)
          (let [path-to-update (vec (concat (interpose :children firn-under) [:children]))
                ;; NOTE: we are doing merges within an update so that parents
                ;; don't overwrite children. basically, the merge has to happen
                ;; at the (vals) level, and then be re-set to the existing node.
                ;; Incoming vals are <string><map>!
                update-fn      (fn [existing-site-map-node title head]
                                 (let [vals1       (get existing-site-map-node title {})
                                       merged-vals (merge vals1 head)
                                       final       (hash-map title merged-vals)]
                                   (merge existing-site-map-node final)))]
            (recur tail (update-in out path-to-update update-fn title head)))

          :else
          (recur tail out)))
      out)))

(defn process-one
  "Munge the 'file' datastructure; slowly filling it up, using let-shadowing.
  Essentially, converts `org-mode file string` -> json, edn, logbook, keywords"
  [config f]

  (let [new-file      (file/make config f)                                     ; make an empty "file" map.
        as-json       (->> f slurp org/parse!)                                 ; slurp the contents of a file and parse it to json.
        as-edn        (-> as-json (json/parse-string true))                    ; convert the json to edn.
        new-file      (file/change new-file {:as-json as-json :as-edn as-edn}) ; shadow the new-file to add the json and edn.
        file-metadata (file/extract-metadata new-file)                         ; collect the file-metadata from the edn tree.
        new-file      (file/change new-file {:meta file-metadata})             ; shadow the file and add the metadata
        ;; TODO PERF: htmlify happening as well in `process-all`.
        ;; this is due to the dev server hot reload.
        ;; There should be a conditional that checks if we are running in server.
        final-file    (htmlify config new-file)]                   ; parses the edn tree -> html.

    final-file))

(defn process-all ; (ie, just org-files, not pages)
  "Receives config, processes all ORG files and builds up site-data logbooks, site-map, link-map, etc.
  This is where the magic happens for collecting metadata. Follow the chain:
  process-all -> process-one -> file/extract-metadata -> file/extract-metadata-helper"
  [config]
  (loop [org-files (config :org-files)
         site-vals {:processed-files  {}
                    :site-map         [] ;; < collected as list, transformed later to map.
                    :site-tags        [] ;; org-headline tags
                    :site-firn-tags   {} ;; file-specific tags (#+ROAM-TAGS or #+FIRN-TAGS)
                    :site-links       []
                    :site-attachments []}
         output    {}]
    (if (empty? org-files)
      ;; run one more loop on all files, and create their html,
      ;; now that we have processed everything.
      (let [config-with-data (merge config
                                    site-vals ;; contains logbook already
                                    {:processed-files (vals output)
                                     :site-map        (make-site-map (site-vals :site-map))
                                     :site-tags       (into (sorted-map) (group-by :tag-value (site-vals :site-tags)))})

            ;; FIXME: I think we are rendering html twice here, should prob only happen here?
            with-html (into {} (for [[k pf] output] [k (htmlify config-with-data pf)]))
            final     (assoc config-with-data :processed-files with-html)]
        final)

      ;; Otherwise continue...
      (let [next-file                                (first org-files)
            processed-file                           (process-one config next-file)
            is-private                               (file/is-private? config processed-file)
            in-sitemap?                              (file/in-site-map? processed-file)
            org-files                                (rest org-files)
            {:keys [links logbook tags attachments firn-tags]} (-> processed-file :meta)]
        (if is-private
          (recur org-files site-vals output)
          (let [updated-output    (assoc output (processed-file :path-web) processed-file)
                updated-site-vals (cond-> site-vals
                                    true        (update :site-links concat links)
                                    true        (update :site-logs concat logbook)
                                    true        (update :site-attachments concat attachments)
                                    true        (update :site-tags concat tags)
                                    in-sitemap? (update :site-map conj (file/make-site-map-item processed-file (-> config :user-config :site-url))))]
            (recur org-files updated-site-vals updated-output)))))))

(defn write-rss-file!
  "Build an rss file. It sorts files by file:meta:date-created, writes to feed.xml"
  [{:keys [processed-files dir-site user-config] :as config}]
  (println "Building rss file...")
  (let [{:keys [site-title site-url site-desc]} user-config
        feed-file   (str dir-site "feed.xml")
        first-entry {:title site-title :link site-url :description site-desc}
        make-rss    (fn [[_ f]]
                      (hash-map :title       (-> f :meta :title)
                                :link        (f :path-web)
                                :pubDate     (u/org-date->java-date  (-> f :meta :date-created))
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

(defn write-pages!
  "Responsible for publishing html pages from clojure templates found in pages/
  Currently, we can only render a flat file list of .clj files in /pages.
  TODO: (In a later release) - do something similar to `file/get-web-path` and
  enable `load-fns-into-map` to save filenames as :namespaced/keys, allowing
  make-parent to work on it."
  [{:keys [dir-site pages partials site-map site-links site-logs site-tags user-config] :as config}]
  (let [site-url (user-config :site-url)
        user-api {:partials   partials
                  :site-links site-links
                  :site-map   site-map
                  :site-logs  site-logs
                  :site-tags  site-tags
                  :site-url   site-url
                  :build-url  (layout/build-url site-url)
                  :config     config}]

    (doseq [[k f] pages
            :let  [out-file (str dir-site "/" (name k) ".html")
                   out-str  (h/html (f user-api))]]
      (io/make-parents out-file)
      (spit out-file out-str)))
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

(defn remove-unused-attachments
  "Deletes all attachments in the _site/<dir-data> that aren't found in the
  site-wide collected attachment paths."
  [{:keys [attachments dir run-build-clean?]}]
  (let [dir-files             (u/find-files dir #"(.*)\.(jpg|JPG|gif|GIF|png)")
        clean-file-link-regex #"(file:)((.*\.)\.\/?)?"
        attachments           (map #(str/replace-first % clean-file-link-regex "") attachments)
        unused                (atom [])]
    ;; find unused files.
    (doseq [f    dir-files
            :let [f-path (.getPath ^java.io.File f)
                  match (u/find-first #(str/includes? f-path %) attachments)]]
      (when (nil? match)
        (swap! unused conj {:full-path  f-path
                            :short-path (u/drop-path-until f-path "_site")})))

    ;; when we have some, delete automatically if `always`, otherwise, prompt user.
    (when (seq @unused)
      (if (= run-build-clean? "always")
        (doseq [f @unused] (fs/delete (f :full-path)))
        (do
          (println "\n" (count @unused) "unused attachments were found (they are not linked to from your org-files):\n")
          (doseq [f @unused] (println (f :short-path)))
          (let [res (u/prompt? "\nDo you want to delete these files?")]
            (if res
              (do
                (println "\nOk, cleaning " dir " directory of unusued attachments...")
                (doseq [f @unused] (fs/delete (f :full-path))))
              (println "Leaving files in place."))))))))

(defn post-build-clean
  "Clean up fn for after a site is built."
  [{:keys [site-attachments user-config dir-site-data] :as config}]
  (let [{:keys [run-build-clean? dir-data]} user-config
        prompt       (str "Would you like to scan for unused attachments in _site/" dir-data "?")
        clean-params {:attachments site-attachments :dir dir-site-data :run-build-clean? run-build-clean?}]
    (case run-build-clean?
      "never"  nil
      "always" (remove-unused-attachments clean-params)
      "prompt" (when (= run-build-clean? "prompt")
                 (when (u/prompt? prompt)
                   (remove-unused-attachments clean-params)))
      nil)
    config))

(defn all-files
  "Processes all files in the org-directory"
  [cfg]
  (let [config     (setup (config/prepare cfg))
        rss?       (-> config :user-config :enable-rss?)
        is-server? (cfg :--server?)]
    (cond->> config
      true             process-all
      rss?             write-rss-file!
      true             write-pages!
      true             write-files
      (nil? is-server?) post-build-clean)))

