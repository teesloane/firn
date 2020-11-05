(ns firn.config
  (:require [clojure.string :as s]
            [firn.util :as u]
            [me.raynes.fs :as fs]
            [sci.core :as sci]))

(def starting-external-config
  (read-string (slurp "resources/firn/_firn_starter/config.edn")))

(defn make-dir-firn
  "make the _firn directory path."
  [dir]
  (str dir "/_firn"))

;; internal configuration for the operation of firn.
(defn make-internal-config
  [dir ext-config]
  (let [mdp             #(str dir "/_firn" %)
        parent-dir-name (-> dir (s/split #"/") last)
        dir-site-data   (mdp (str "/_site/" (ext-config :dir-data)))] ; make-dir-path
    {;; Paths to common directories
     :dir-data        (str dir "/" (ext-config :dir-data))
     :dir             dir                    ; where org content lives.
     :dir-firn        (make-dir-firn dir)    ; the _firn root folder.
     :dir-layouts     (mdp "/layouts/")      ; where layouts are stored.
     :dir-partials    (mdp "/partials/")     ; where partials are stored.
     :dir-pages       (mdp "/pages/")        ; where pages are stored (tags.clj, user pages, etc)
     :dir-site        (mdp "/_site/")        ; the root dir of the compiled firn site.
     :dir-site-data   dir-site-data          ; _site data folder output.
     :dir-site-static (mdp "/_site/static/") ; _site static output for dir-static.
     :dir-static      (mdp "/static/")       ; static folder for css/js
     :dirname-files   parent-dir-name        ; the name of directory where firn is run.
     :layouts         {}                     ; layouts loaded into memory - ie, org -> clj -> html
     :pages           {}                     ; layouts sans org-mode files - ie, clj -> html
     :org-files       []                     ; a list of org files, fetched when running setup.
     :user-config     {}                     ; user's config.edn values.
     :partials        {}                     ; partials loaded into memory

     ;; values collected during build/process-all:
     :processed-files  []
     :site-map         []                        ; list of all pages converted for the site.
     :org-tags        {}                        ; collected tags from all processed files
     :site-logs        []                        ; collected logs from all processed files
     :site-links       []                        ; collected links from all processed files
     :site-attachments []}))                     ; collected (paths to) attachments of all processed files.

;; Values that a user can contribute/change via their config.edn
(defn make-external-config
  "Reads a user's config and merges it into the starting external"
  [dir]
  (let [user-cfg-path (str (make-dir-firn dir) "/config.edn")]
    (if-not (fs/exists? user-cfg-path)
      (u/print-err! :error "Didn't find a _firn site in this directory. Have you run `firn new` yet?")
      (try ; to read user config
        (->> user-cfg-path
           (slurp)
           (sci/eval-string)
           (merge starting-external-config))

        (catch Exception ex
          (println "Failed to read 'config.edn' file - is it properly formatted?"))))))

(defn prop
  [config kwrd]
  (case kwrd
    :site-url     (-> config :user-config :site-url)
    :site-title   (-> config :user-config :site-title)
    :site-author  (-> config :user-config :site-author)
    :site-desc    (-> config :user-config :site-desc)
    :ignored-dirs (-> config :user-config :ignored-dirs)))

(defn prepare
  "Prepares the configuration for build/serve."
  [{:keys [dir --server? port]}]
  (let [ext-config   (make-external-config dir)
        int-config   (make-internal-config dir ext-config)
        final-config (assoc int-config :user-config ext-config)]
    (if --server?
      (assoc-in final-config [:user-config :site-url] (str "http://localhost:" port))
      final-config)))
