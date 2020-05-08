(ns firn.server
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [firn.config :as config]
            [firn.file :as file]
            [juxt.dirwatch :refer [watch-dir close-watcher]]
            [firn.util :as u]
            [firn.build :as build]
            [me.raynes.fs :as fs]
            [mount.core :as mount :refer [defstate]]
            [org.httpkit.server :as http]
            [ring.middleware.file :as r-file]
            [ring.util.response :refer [response]]))


(declare server)
(def file-watcher  (atom nil))

(defn- prep-uri
  [req]
  (let [stripped (s/join "" (-> req :uri rest))]
    (u/remove-ext stripped "html")))

(defn handler
  "Handles web requests for the development server.
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

(defn- watcher-dir-action
  "The watcher has to sometimes copy whole directories,
  say if a folder gets dropped into the watched folders."
  [file dest dir-site action]
  (let [file-path      (.getPath ^java.io.File file)
        differing-path (u/get-differing-path dest file-path)
        final-path     (str dir-site differing-path)]
    (case action
      :delete
      (if (fs/directory? file)
        (fs/delete-dir final-path) ;; this doesn't seem to be working.
        (fs/delete final-path))

      :modxcreate
      (if (fs/directory? file)
        (do (fs/delete-dir final-path)
            (fs/copy-dir file final-path))
        (do
          (fs/delete final-path)
          (fs/copy+ file final-path))))))

(defn handle-watcher
  "Handles reloading. Expects `config!` to be partially applied.
  Whenever a file changes, as set up by `watch-dir`, run this fn."
  [config! {:keys [file action]}]
  (let [file-path            (.getPath ^java.io.File file)
        file-name-as-kywrd   (u/io-file->keyword file)
        f-modxcreate         (or (= action :modify) (= action :create))
        f-actions            {:modxcreate f-modxcreate :delete (= action :delete)}
        match-dir-and-action #(and (s/includes? file-path %1) (get f-actions %2))

        ;; The dirs we are moving things to<->from
        {:keys [dir-partials    dir-layouts
                dir-static      dir-site
                dir-site-static dir-attach
                dir-site-data]} @config!]

    (prn "Reloading files..." file-path)
    (cond
      (match-dir-and-action dir-partials :modxcreate)
      (swap! config! assoc-in [:partials file-name-as-kywrd] (u/read-and-eval-clj file))

      (match-dir-and-action dir-layouts :modxcreate)
      (swap! config! assoc-in [:layouts file-name-as-kywrd] (u/read-and-eval-clj file))

      (match-dir-and-action dir-attach :modxcreate)
      (watcher-dir-action file dir-site-data dir-site :modxcreate)

      (match-dir-and-action dir-static :modxcreate)
      (watcher-dir-action file dir-site-static dir-site :modxcreate)

      (match-dir-and-action dir-attach :delete)
      (watcher-dir-action file dir-site-data dir-site :delete)

      (match-dir-and-action dir-static :delete)
      (watcher-dir-action file dir-site-static dir-site :delete)

      ;; NOTE: 🐛 🐛  Might be a race condition with the watchers:
      ;; either way, `modify` actions are still showing up after the `delete`
      ;; shows up.

      (match-dir-and-action dir-partials :delete)
      (swap! config! update :partials dissoc file-name-as-kywrd)

      (match-dir-and-action dir-layouts :delete)
      (swap! config! update :layouts dissoc file-name-as-kywrd))))


(defstate server
  :start
  (let [args         (mount/args)
        dir-files    (get args :path (u/get-cwd))
        path-to-site (str dir-files "/_firn/_site")
        ;; build all files and prepare a mutable config (for reloading)
        config!      (atom (-> dir-files config/prepare build/setup file/process-all))
        {:keys       [dir-layouts dir-partials dir-static dir-attach]} @config!
        watch-list   (map io/file [dir-layouts dir-partials dir-static dir-attach])
        port         3333]

    ;; start watchers
    (reset! file-watcher (apply watch-dir (partial handle-watcher config!) watch-list))

    (println "Building site...")
    (if-not (fs/exists? path-to-site)
      (println "Couldn't find a _firn/ folder. Have you run `Firn new` and created a site yet?")
      (do (println "⚠ The Firn development server is in beta. \n You may need to restart from time to time if you run into issues.")
          (println "🏔 Starting Firn development server on:" port)
          (http/run-server (handler config!) {:port port}))))

  :stop
  (do
    (server :timeout 100)
    (when @file-watcher
      (close-watcher @file-watcher)
      (reset! file-watcher nil))))

;; -- Repl Land --

(defn serve
  [opts]
  (mount/start-with-args opts)
  (promise)) ; NOTE: this is for CLI-matic stuff for now.)

;; (serve {:path "/Users/tees/Projects/firn/firn/clojure/test/firn/demo_org"})
;; (mount/stop)
