(ns firn.config
  (:require [clojure.string :as s]
            [sci.core :as sci]
            [firn.util :as u]
            [me.raynes.fs :as fs]))

(def starting-config
  {:dir-attach    "data"    ; org attachments/files to get copied into _site.
   :dir-files     nil       ; where org content lives.
   :dir-layouts   ""        ; where layouts are stored.
   :dir-partials  ""        ; where partials are stored.
   :dir-site      ""        ; the root dir of the compiled firn site.
   :dirname-files nil       ; the name of directory where firn is run.
   :ignored-dirs  ["priv"]  ; Directories to ignore org files in.
   :layouts       {}        ; layouts loaded into memory
   :partials      {}        ; partials loaded into memory
   :org-files     []})      ; a list of org files, fetched when running setup.


;; -- Default Config -----------------------------------------------------------


(defn make-dir-firn
  "make the _firn directory path."
  [dir-files]
  (str dir-files "/_firn"))

(defn default
  "Assume that files-dir does NOT end in a `/`ex: /Users/tees/Dropbox/wiki"
  [dir-files]
  (merge starting-config
         {:dir-firn        (make-dir-firn dir-files)
          :dir-attach      (str dir-files "/" (starting-config :dir-attach))
          :dir-files       dir-files
          :dir-layouts     (str dir-files "/_firn/layouts/")
          :dir-partials    (str dir-files "/_firn/partials/")
          :dir-site        (str dir-files "/_firn/_site/")
          :dir-site-attach (str dir-files "/_firn/_site/" (starting-config :dir-attach))
          :dir-site-static (str dir-files "/_firn/_site/static/")
          :dir-static      (str dir-files "/_firn/static/")
          :dirname-files   (-> dir-files (s/split #"/") last)})) ;; the name of the dir where files are.

(defn clean-config
  "Takes the user config and strips any keys from it that shouldn't be changed
  in the internal config.
  NOTE: Write tests for this."
  [cfg]
  (let [permanent-keys #{:dir-firn          :dir-layouts   :dir-partials
                         :dir-static        :dir-site      :dir-site-static
                         :dir-site-attach   :dir-files     :dirname-files
                         :layouts           :org-files     :partials}]
    (apply dissoc cfg (filter #(contains? cfg %) permanent-keys))))

(defn prepare
  "Takes a path to directory of org files and prepares the
  firn-wide config map to pass through functions.
  Also checks for a config.edn at _firn dir and merges it in to the default config.
  Assume that files-dir does NOT end in a `/`ex: /Users/tees/Dropbox/wiki "
  [dir-files]
  (let [wiki-path      (if (empty? dir-files) (u/get-cwd) dir-files)
        default-config (default wiki-path)
        path-to-config (str (default-config :dir-firn) "/config.edn")]
    (if-not (fs/exists? path-to-config)
      ;; No config found
      (do
        (println "Didn't find a _firn site. Have you run `firn new` yet?")
        #_(System/exit 0)) ;; TODO: good place for a "if DEV..." so the repl doesn't close.
      ;; try and read the config
      (try
        (let [read-config    (sci/eval-string (slurp (str (default-config :dir-firn) "/config.edn")))
              cleaned-config (clean-config read-config)
              merged-config  (merge default-config cleaned-config)]
          merged-config)
        (catch Exception ex
          (println
           "Failed to read 'config.edn' file - is it properly formatted?"))))))
