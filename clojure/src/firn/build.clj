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

    ;; copy attachments and static files too final _site dir.
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

(defn file-server-middleware
  [{:keys [dir-site dir-files] :as config}]

  (fn [request]
    ;; This is naive; everytime we visit a page it runs the build step.
    (let [res-file-system ((r-file/wrap-file request dir-site) request)
          req-uri-file    (s/join "" (-> request :uri rest)) ;; we have to trim the requested uri because it comes in as "/my-link"; but they are in memory as "my-link"
          file-html       (get-in config [:processed-files req-uri-file :as-html])
          four-oh-four    {:status 404 :body "File not found."}]
      ;; TODO: - re-process single file when hit by route.
      ;; TODO: - all links with .html in them need to be stripped?
      ;; TODO: - make sure index is rendering from memory?
      (cond
        (some? file-html)       (response file-html)
        (some? res-file-system) res-file-system
        :else                   four-oh-four))))

(defstate server
  :start (let [args         (mount/args)
               dir-files    (get args :-path (u/get-cwd))
               path-to-site (str dir-files "_firn/_site")
               ;; config       (setup (config/prepare dir-files))
               config       (-> (config/prepare dir-files) setup file/process-all) ;; basically doing `all-files`
               port         3333]
           (println "Building site...")
           ;; (all-files {:dir-files dir-files}) ;; we don't need this; we hold files in memory rn
           (if-not (fs/exists? path-to-site)
             (println "Couldn't find a _firn/ folder. Have you run `Firn new` and created a site yet?")
             (do (println "🏔 Starting Firn development server on:" port)
                 (http/run-server (file-server-middleware config) {:port port}))))
  :stop (when server (server :timeout 100)))

(defn serve
  [opts]
  ;; TODO: build the whole site before running the server.
  (mount/start-with-args opts))

;; (serve {:-path "/Users/tees/Dropbox/wiki/"})
