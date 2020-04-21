(ns firn.config
  (:require [clojure.string :as s]))


(def starting-config
  {:dir-attach    "data"        ; org attachments/files to get copied into _site.
   :dir-files     nil           ; where org content lives.
   :dir-layouts   ""            ; where layouts are stored.
   :dir-partials  ""            ; where partials are stored.
   :dirname-files nil           ; the name of directory where firn is run.
   :dirname-out   "_firn/_site" ; the root dir of the compiled firn site.
   :ignored-dirs  ["priv"]      ; Directories to ignore org files in.
   :layouts       {}            ; layouts loaded into memory
   :org-files     nil})         ; a list of org files, fetched when running setup.


;; -- Default Config -----------------------------------------------------------

(defn default
  "Assume that files-dir does NOT end in a `/`
   ex: /Users/tees/Dropbox/wiki"
  [dir-files]
  (merge starting-config
         {:dir-firn        (str dir-files "/_firn")
          :dir-layouts     (str dir-files "/_firn/layouts/")
          :dir-partials    (str dir-files "/_firn/partials/")
          :dir-static      (str dir-files "/_firn/static/")
          :dir-attach      (str dir-files "/" (starting-config :dir-attach))
          :dir-site        (str dir-files "/_firn/_site/")
          :dir-site-static (str dir-files "/_firn/_site/static/")
          :dir-site-attach (str dir-files "/_firn/_site/" (starting-config :dir-attach))
          :dir-files       dir-files
          :dirname-files   (-> dir-files (s/split #"/") last) ;; the name of the dir where files are.
          :parser-path     (str dir-files "/_firn/bin/parser")}))
