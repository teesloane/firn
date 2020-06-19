(ns firn.config
  (:require [clojure.string :as s]
            [firn.util :as u]
            [me.raynes.fs :as fs]
            [sci.core :as sci]))

;; TODO: this gets written to resources starter config.
(def starting-external-config
  {:dir-data        "data"   ; org-attachments/files to get copied into _site.
   :enable-rss?     true     ; If true, creates a feed.xml in _site.
   :firn-properties false    ; whether to render properties under headings
   ;; :firn-toc     ...      ; TODO
   ;; :firn-fold    ...      ; TODO
   :ignored-dirs    ["priv"] ; Directories to ignore org files in.
   :site-desc       ""       ; Used for RSS.
   :site-title      ""       ; Used for RSS.
   :site-url        ""})

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
    {:dir-data        (str dir-files "/" (ext-config :dir-data))
     :dir-files       dir-files                 ; where org content lives.
     :dir-firn        (make-dir-firn dir-files) ; the _firn root folder.
     :dir-layouts     (mdp "/layouts/")         ; where layouts are stored.
     :dir-partials    (mdp "/partials/")        ; where partials are stored.
     :dir-site        (mdp "/_site/")           ; the root dir of the compiled firn site.
     :dir-site-data   dir-site-data             ; _site data folder output.
     :dir-site-static (mdp "/_site/static/")    ; _site static output for dir-static.
     :dir-static      (mdp "/static/")    ; static folder for css/js
     :dirname-files   parent-dir-name           ; the name of directory where firn is run.
     :layouts         {}                        ; layouts loaded into memory
     :org-files       []                        ; a list of org files, fetched when running setup.
     :user-config     {}                        ; user's config.edn values.
     :partials        {}}))                        ; partials loaded into memory


;; Values that a user can contribute/change via their config.edn
(defn make-external-config
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
  [dir-files]
  (let [ext-config (make-external-config dir-files)
        int-config (make-internal-config dir-files ext-config)]
    (assoc int-config :user-config ext-config)))

