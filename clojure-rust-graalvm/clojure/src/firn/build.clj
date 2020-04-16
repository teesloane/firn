(ns firn.build
  (:require [firn.config :as config]
            [me.raynes.fs :as fs]
            [clojure.string :as s]
            [cpath-clj.core :as cp]
            [clojure.java.io :as io])
  (:import [iceshelf.clojure.rust ClojureRust]))

(set! *warn-on-reflection* true)

(defn get-cwd
  "Because *fs/cwd* gives out the at-the-time jvm path.
  this works with graal."
  []
  (s/join "/" (-> (java.io.File. ".")
                  .getAbsolutePath
                  (s/split #"/")
                  drop-last)))

(defn ->parser
  "Send a string to the parser. Returns parsed string."
  ;; TODO rename getFreeMemory to parse-org or whatever.
  [s]
  (ClojureRust/getFreeMemory s))


(defn prepare-config
  "Takes a path to files (or CWD) and makes a config with it."
  [_path] ;; FIXME remove
  (let [; path   (if (empty? path) (.getPath ^java.io.File fs/*cwd*) path)
        path   (get-cwd)
        config (config/default path)]
    config))

(defn copy-site-template!
  "Takes the default site template and copies files to the dir-firn in confing
  NOTE: doing this because it's really frustrating / hard to figure out how
  to copy files from a compiled jar / native image from a resources directory."
  [dir-out]
  (let [base-dir        "firn/_firn_starter/"
        files           ["layouts/default.clj" "partials/head.clj" "partials/nav.clj"]
        read-files      (map #(hash-map :contents (slurp (io/resource (str base-dir %)))
                                        :out-name (str dir-out "/" %)) files)]
    (doseq [f read-files]
      (io/make-parents (:out-name f))
      (spit (:out-name f) (:contents f)))))




(defn new-site
  "Creates the folders needed for a new site in your wiki directory.
  Copies the _firn_starter from resources, into where you are running the cmd."
  [path]
  (let [new-config (-> path prepare-config)
        config     new-config
        dir-firn   (config :dir-firn)]

    (let [the-test (io/resource "foo.clj")]
      (println "teh result of calling the eval is" the-test))

    (if (fs/exists? dir-firn)
      (println "A _firn directory already exists.")
      (do (fs/mkdir dir-firn)
          (copy-site-template! dir-firn)))))

;; (prn "io/resource " (io/resource "_firn_starter"))
;; (slurp (io/resource "layouts/default.clj"))
;; (io/resource "layouts/default.clj")

;; (io/resource "foo.clj")
