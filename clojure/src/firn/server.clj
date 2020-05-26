(ns firn.server
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [firn.build :as build]
            [firn.config :as config]
            [firn.file :as file]
            [firn.util :as u]
            [firn.dirwatch :refer [close-watcher watch-dir]]
            [me.raynes.fs :as fs]
            [mount.core :as mount :refer [defstate]]
            [org.httpkit.server :as http]
            [ring.middleware.file :as r-file]
            [ring.util.response :refer [response]]))

(declare server)
(def file-watcher  (atom nil))

(defn- prep-uri
  "Strips a uri of preceding `/` and then removes `.html` if it exists.
  `/this-is/my-req.html` -> `this-is/my-req`"
  [req]
  (let [stripped (s/join "" (-> req :uri rest))]
    (u/remove-ext stripped "html")))

(defn handler
  "Handles web requests for the development server."
  [config!]
  (fn [request]
    (let [dir-site        (get @config! :dir-site)
          res-file-system ((r-file/wrap-file request dir-site) request)     ; look for file in FS
          req-uri-file    (prep-uri request)
          memory-file     (get-in @config! [:processed-files req-uri-file]) ; use the uri to pull values out of memory in config
          index-file      (get-in @config! [:processed-files "index"])      ; use the uri to pull values out of memory in config
          four-oh-four    {:status 404 :body "File not found."}]            ; a ring response for when nothing is found.

      (cond
        ;; Handle reloading of the index / no uri
        (and (= req-uri-file "") (some? index-file))
        (let [reloaded-file (file/reload-requested-file index-file @config!)] ; reslurp in case it has changed.
          (response (reloaded-file :as-html)))

        ;; Handle when the route matches a file in memory
        (some? memory-file)                    ; If req-uri finds the file in the config's memory...
        (let [reloaded-file (file/reload-requested-file memory-file @config!)] ; reslurp in case it has changed.
          (response (reloaded-file :as-html)))

        ;; Handle loading from file system if nothign else found.
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
                dir-site-static dir-data
                dir-site-data]} @config!]

    (prn "Reloading files..." file-path)

    (cond
      (match-dir-and-action dir-partials :modxcreate)
      (swap! config! assoc-in [:partials file-name-as-kywrd] (u/read-and-eval-clj file))

      (match-dir-and-action dir-layouts :modxcreate)
      (swap! config! assoc-in [:layouts file-name-as-kywrd] (u/read-and-eval-clj file))

      (match-dir-and-action dir-data :modxcreate)
      (watcher-dir-action file dir-site-data dir-site :modxcreate)

      (match-dir-and-action dir-static :modxcreate)
      (watcher-dir-action file dir-site-static dir-site :modxcreate)

      (match-dir-and-action dir-data :delete)
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
  (let [{:keys [dir port]
         :or   {dir (u/get-cwd)
                port 3333}}           (mount/args)
        path-to-site                  (str dir "/_firn/_site")
        ;; NOTE: consider making this global, and so available to a sci repl?
        config!                       (atom (-> dir config/prepare build/setup file/process-all))
        {:keys [dir-layouts dir-partials dir-static dir-data]} @config!
        watch-list                    (map io/file [dir-layouts dir-partials dir-static dir-data])]

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

(defn serve
  ([]
   (serve {}))
  ([opts]
   (mount/start-with-args opts)))

;; -- Repl Land --

;; (mount/stop)
