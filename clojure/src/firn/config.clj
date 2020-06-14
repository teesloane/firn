(ns firn.config
  (:require [clojure.string :as s]
            [firn.util :as u]
            [me.raynes.fs :as fs]
            [sci.core :as sci]))

(def starting-config
  {:dir-data      "data"   ; org attachments/files to get copied into _site.
   :dir-files     nil      ; where org content lives.
   :dir-layouts   ""       ; where layouts are stored.
   :dir-partials  ""       ; where partials are stored.
   :dir-site      ""       ; the root dir of the compiled firn site.
   :dirname-files nil      ; the name of directory where firn is run.
   :site-url      ""       ; Root level url
   :site-title    ""       ; Used for RSS.
   :site-desc     ""       ; Used for RSS.
   :enable-rss?   true     ; If true, creates a feed.xml in _site.
   :ignored-dirs  ["priv"] ; Directories to ignore org files in.
   :layouts       {}       ; layouts loaded into memory
   :partials      {}       ; partials loaded into memory
   :org-files     []})      ; a list of org files, fetched when running setup.

(defn make-dir-firn
  "make the _firn directory path."
  [dir-files]
  (str dir-files "/_firn"))

(defn default
  "Assume that files-dir does NOT end in a `/`ex: /Users/tees/Dropbox/wiki"
  ([dir-files]
   (default dir-files {}))

  ([dir-files external-config]
   (let [base-config (merge starting-config external-config)]
     (merge base-config
            {:dir-firn        (make-dir-firn dir-files)
             :dir-data      (str dir-files "/" (base-config :dir-data))
             :dir-files       dir-files
             :dir-layouts     (str dir-files "/_firn/layouts/")
             :dir-partials    (str dir-files "/_firn/partials/")
             ;; all outputted _site directories.
             :dir-site        (str dir-files "/_firn/_site/")
             :dir-site-data (str dir-files "/_firn/_site/" (base-config :dir-data))
             :dir-site-static (str dir-files "/_firn/_site/static/")
             :dir-static      (str dir-files "/_firn/static/")
             :dirname-files   (-> dir-files (s/split #"/") last)})))) ;; the name of the dir where files are.

(defn clean-config
  "Takes the user config and strips any keys from it that shouldn't be changed
  in the internal config before they get merged together."
  [cfg]
  (let [permanent-keys #{:dir-firn          :dir-layouts   :dir-partials
                         :dir-static        :dir-site      :dir-site-static
                         :dir-site-data     :dir-files     :dirname-files
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
      (u/print-err! :error "Didn't find a _firn site in this directory. Have you run `firn new` yet?")
      (try ;; to read config
        (let [read-config    (sci/eval-string (slurp (str (default-config :dir-firn) "/config.edn")))
              cleaned-config (clean-config read-config)
              final-config   (default wiki-path cleaned-config)]
          final-config)
        (catch Exception ex
          (println
           "Failed to read 'config.edn' file - is it properly formatted?"))))))
