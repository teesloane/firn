(ns firn.config
  (:require [clojure.string :as s]
            [firn.util :as u]
            [me.raynes.fs :as fs]
            [sci.core :as sci]))

(def starting-external-config
  (read-string (slurp "resources/firn/_firn_starter/config.edn")))

(defn make-dir-firn
  "make the _firn directory path."
  [dir-files]
  (str dir-files "/_firn"))

;; internal configuration for the operation of firn.
(defn make-internal-config
  [dir-files ext-config]
  (let [mdp             #(str dir-files "/_firn" %)
        parent-dir-name (-> dir-files (s/split #"/") last)
        dir-site-data   (mdp (str "/_site/" (ext-config :dir-data)))] ; make-dir-path
    {;; Paths to common directories
     :dir-data         (str dir-files "/" (ext-config :dir-data))
     :dir-files        dir-files                 ; where org content lives.
     :dir-firn         (make-dir-firn dir-files) ; the _firn root folder.
     :dir-layouts      (mdp "/layouts/")         ; where layouts are stored.
     :dir-partials     (mdp "/partials/")        ; where partials are stored.
     :dir-pages        (mdp "/pages/")           ; where pages are stored (tags.clj, user pages, etc)
     :dir-site         (mdp "/_site/")           ; the root dir of the compiled firn site.
     :dir-site-data    dir-site-data             ; _site data folder output.
     :dir-site-static  (mdp "/_site/static/")    ; _site static output for dir-static.
     :dir-static       (mdp "/static/")          ; static folder for css/js
     :dirname-files    parent-dir-name           ; the name of directory where firn is run.
     :layouts          {}                        ; layouts loaded into memory - ie, org -> clj -> html
     :pages            {}                        ; layouts sans org-mode files - ie, clj -> html
     :org-files        []                        ; a list of org files, fetched when running setup.
     :user-config      {}                        ; user's config.edn values.
     :partials         {}                        ; partials loaded into memory

     ;; values collected during build/process-all:
     :processed-files  []
     :site-map         []                        ; list of all pages converted for the site.
     :site-tags        {}                        ; collected tags from all processed files
     :site-logs        []                        ; collected logs from all processed files
     :site-links       []                        ; collected links from all processed files
     :site-attachments []}))                     ; collected (paths to) attachments of all processed files.

;; Values that a user can contribute/change via their config.edn
(defn make-external-config
  "Reads a user's config and merges it into the starting external"
  [dir-files]
  (let [user-cfg-path (str (make-dir-firn dir-files) "/config.edn")]
    (if-not (fs/exists? user-cfg-path)
      (u/print-err! :error "Didn't find a _firn site in this directory. Have you run `firn new` yet?")
      (try ; to read user config
        (->> user-cfg-path
           (slurp)
           (sci/eval-string)
           (merge starting-external-config))

        (catch Exception ex
          (println "Failed to read 'config.edn' file - is it properly formatted?"))))))

(defn prepare
  "Prepares the configuration for build/serve."
  [{:keys [dir --server?]}]
  (let [ext-config   (make-external-config dir)
        int-config   (make-internal-config dir ext-config)
        final-config (assoc int-config :user-config ext-config)]
    (if --server?
      (assoc-in final-config [:user-config :site-url] "http://localhost:4000")
      final-config)))
