(ns firn.build
  "Provides functions to core, to be called in the cli.
  Mostly to do with the processing of files / new site."
  (:require [clojure.java.io :as io]
            [firn.config :as config]
            [firn.file :as file]
            [firn.util :as u]
            [mount.core :refer [defstate] :as mount]
            [org.httpkit.server :as http]
            ;; [reitit.ring :as ring]
            [ring.middleware.file :as r-file]
            [ring.util.response :refer [response]]
            ;; [ring.adapter.jetty :as jetty]
            [me.raynes.fs :as fs]
            [clojure.string :as s]))

(set! *warn-on-reflection* true)
(declare server)

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

(defn copy-static-files
  [config]
  (when-not (fs/exists? (config :dir-site-attach))
    (fs/copy-dir (config :dir-attach) (config :dir-site-attach)))
  (when-not (fs/exists? (config :dir-site-static))
    (fs/copy-dir (config :dir-static) (config :dir-site-static))))

(defn setup
  "Creates folders for output, slurps in layouts and partials.
  NOTE: should slurp/mkdir/copy-dir be wrapped in try-catches? if-err handling?"
  [{:keys [dir-layouts dir-partials dir-files] :as config}]
  (when-not (fs/exists? (config :dir-firn)) (new-site config))

  (let [layout-files  (u/find-files-by-ext dir-layouts "clj")
        partial-files (u/find-files-by-ext dir-partials "clj")
        partials-map  (u/load-fns-into-map partial-files)
        org-files     (u/find-files-by-ext dir-files "org") ;; could bail if this is empty...
        layouts-map   (u/load-fns-into-map layout-files)]

    (fs/mkdir (config :dir-site)) ;; make _site

    ;; copy attachments and static files to final _site dir.
    (when-not (fs/exists? (config :dir-site-attach))
      (fs/copy-dir (config :dir-attach) (config :dir-site-attach)))
    (when-not (fs/exists? (config :dir-site-static))
      (fs/copy-dir (config :dir-static) (config :dir-site-static)))

    (assoc
     config :org-files org-files :layouts layouts-map :partials partials-map)))

(defn write-files
  "Takes a config, of which we can presume has :processed-files.
  Iterates on these files, and writes them to html using layouts."
  [config]
  (doseq [[_ f] (config :processed-files)]
    (let [out-file-name (str (config :dir-site) (f :path-web) ".html")]
      (when-not (file/is-private? config f)
        (io/make-parents out-file-name)
        (spit out-file-name (f :as-html))))))

(defn all-files
  "Processes all files in the org-directory"
  [{:keys [dir-files]}]
  (let [config (setup (config/prepare dir-files))]
    (->> config
         file/process-all
         write-files)))

;; -- Server --

(defn handler
  "Handles web requests for the development server.
  FIXME: Needs a file watcher for determining when to copy files into dir-site"
  [{:keys [dir-firn dir-site] :as config}]
  (fn [request]
    (let [; first we try and get the request to load from the files system
          res-file-system ((r-file/wrap-file request dir-site) request)
          ;; then we pull the uri out of req and format it: `/this-is/my-req` -> `this-is/my-req`
          req-uri-file    (s/join "" (-> request :uri rest))
          ;; use the uri to pull values out of memory in config
          memory-file     (get-in config [:processed-files req-uri-file])
          ;; a ring response for when nothing is found.
          four-oh-four    {:status 404 :body "File not found."}]
      ;; TODO: - make sure index is rendering from memory?
      (cond
        ;; If the request was found to match in the config...
        (some? memory-file)
        ;; let's re-slurp the file in case it's changed
        ;; someday this will be handled by a file watcher.
        (let [reloaded-file (file/reload-requested-file memory-file config)]
          ;; then we can respones with the reloaded-files's html.
          (response (reloaded-file :as-html)))

        ;; If the file isn't found in memory, let's try using a file in the _firn/_site fs.
        (some? res-file-system)
        res-file-system

        :else
        four-oh-four))))

(defstate server
  :start (let [args         (mount/args)
               dir-files    (get args :-path (u/get-cwd))
               path-to-site (str dir-files "/_firn/_site")
               ;; build all files and prepare a config.
               config       (-> dir-files config/prepare setup file/process-all)
               port         3333]
           (println "Building site...")
           (if-not (fs/exists? path-to-site)
             (println "Couldn't find a _firn/ folder. Have you run `Firn new` and created a site yet?")
             (do (println "🏔 Starting Firn development server on:" port)
                 (http/run-server (handler config) {:port port}))))
  :stop (when server (server :timeout 100)))

(defn serve
  [opts]
  (mount/start-with-args opts))

;; cider won't boot if this is uncommented at jack-in:
;; (serve {:-path "/Users/tees/Projects/firn/firn/clojure/test/firn/demo_org"})
