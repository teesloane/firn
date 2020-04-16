(ns firn.build
  (:require [firn.config :as config]
            [me.raynes.fs :as fs]
            [clojure.string :as s]
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


(defn new-site
  "Creates the folders needed for a new site in your wiki directory.
  Copies the _firn_starter from resources, into where you are running the cmd.
  FIXME: This does not work with JARs - it's complicated to copy entire directories from a jar.
  possible solution: https://stackoverflow.com/a/28682910"
  [path]
  (let [new-config      (-> path prepare-config)
        ;; existing-config (first args)

        config           new-config]
    (clojure.pprint/pprint config)
    (prn "io/resource " (io/resource "_firn_starter"))
    (if (fs/exists? (config :dir-firn))
      (println "A _firn directory already exists.")
      ;; (fs/mkdir (config :dir-firn))
      (fs/copy-dir (io/resource "_firn_starter") (config :dir-firn)))))
