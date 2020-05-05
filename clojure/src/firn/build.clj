(ns firn.build
  "Provides functions to core, to be called in the cli.
  Mostly to do with the processing of files / new site."
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [firn.config :as config]
            [firn.file :as file]
            [juxt.dirwatch :refer [watch-dir close-watcher]]
            [firn.util :as u]
            [sci.core :as sci]
            [me.raynes.fs :as fs]
            [mount.core :as mount :refer [defstate]]
            [org.httpkit.server :as http]
            [ring.middleware.file :as r-file]
            [ring.util.response :refer [response]]))

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

(defn get-clj-files->m
  "Gets layouts and partials and stores them and their slurped contents in a map."
  [config]

  (let [{:keys [dir-layouts dir-partials]} config
        layout-files (u/find-files-by-ext dir-layouts "clj")
        partial-files (u/find-files-by-ext dir-partials "clj")
        partials-map  (u/load-fns-into-map partial-files)
        layouts-map   (u/load-fns-into-map layout-files)]
    {:layouts layouts-map :partials partials-map}))

;; TODO - this isn't being used yet.
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
;; TODO - move all server stuff

(defn- prep-uri
  [req]
  (let [stripped (s/join "" (-> req :uri rest))]
    (u/remove-ext stripped "html")))

(defn handler
  "Handles web requests for the development server.
  FIXME: Needs a file watcher for determining when to copy:
  - anything in static
  - anything in data
  - anything in layouts/partials
  -> into dir-site
  TODO: - make sure index is rendering from memory?"
  [config!]
  (fn [request]
    (let [dir-site        (get @config! :dir-site)
          res-file-system ((r-file/wrap-file request dir-site) request)     ; look for file in FS
          req-uri-file    (prep-uri request)                                ; fmt the uri: `/this-is/my-req.html` -> `this-is/my-req`
          memory-file     (get-in @config! [:processed-files req-uri-file]) ; use the uri to pull values out of memory in config
          four-oh-four    {:status 404 :body "File not found."}]          ; a ring response for when nothing is found.
      (cond

        (some? memory-file)                    ; If req-uri finds the file in the config's memory...
        (let [reloaded-file (file/reload-requested-file memory-file @config!)] ; reslurp in case it has changed.
          (response (reloaded-file :as-html))) ; then we can respones with the reloaded-files's html.

        ;; If the file isn't found in memory, let's try using a file in the _firn/_site fs.
        (some? res-file-system)
        res-file-system

        :else four-oh-four))))

(defn handle-watcher
  "Handles reloading. Expects config to be partially applied."
  [config! watched-file]
  ;; LEAVING OFF - find out if this works (reloading a single file into mem)
  (let [file-path                  (.getPath ^java.io.File (watched-file :file))
        {:keys [partials layouts]} (get-clj-files->m @config!)]

    (swap! config! assoc :partials partials :layouts layouts)
    (println "Reloadinig File... " watched-file)))

(defstate server
  :start
  (let [state!         (mount/args) ;; this is an atom

        dir-files    (get @state! :path (u/get-cwd))
        path-to-site (str dir-files "/_firn/_site")
        ;; build all files and prepare a config.
        config       (-> dir-files config/prepare setup file/process-all)
        ;; config is an atom so we can swap! in reloaded files.
        config!      (atom config)
        {:keys       [dir-layouts dir-partials dir-static]} config
        watch-list   (map io/file [dir-layouts dir-partials dir-static])
        port         3333]

    ;; start watchers
    ;; this needs to happen only once because repl.
    (println "starting file watcher!")
    (swap! state! assoc :watcher (apply watch-dir (partial handle-watcher config!) watch-list))
    (println "stateis " @state!)

    (println "Building site...")
    (if-not (fs/exists? path-to-site)
      (println "Couldn't find a _firn/ folder. Have you run `Firn new` and created a site yet?")
      (do (println "🏔 Starting Firn development server on:" port)
          (http/run-server (handler config!) {:port port}))))

  :stop
  (when server
    (let [state   @(mount/args)
          watcher (state :watcher)]
      (prn "watcher is " watcher)
      (when watcher
        (prn "Disconnecting file watchers." watcher)
        (close-watcher watcher)))

    (prn "Shutting down server")
    (server :timeout 100)))


;; -- Repl Land --


(defn serve
  [opts]
  (mount/start-with-args opts)
  (promise))

;; cider won't boot if this is uncommented at jack-in:
(serve (atom {:path    "/Users/tees/Projects/firn/firn/clojure/test/firn/demo_org"
              :watcher nil}))
;; (serve {:-path "/Users/tees/Dropbox/wiki"})

;; (mount/stop)
