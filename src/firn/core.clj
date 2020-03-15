(ns firn.core
  (:require [cheshire.core :as json]
            [clojure.java.shell :as sh]
            [clojure.string :as s]
            [firn.config :as config]
            [firn.layout :as layout]
            [me.raynes.fs :as fs])
  (:gen-class))

(defn setup
  "Creates output directory for files and sets up starting
  config file that gets passed through all functions
  Also, moves your `media folder` into _site. TODO - make configurable..."
  [config]

  (println "Setup: Making _site output.")
  (fs/mkdir (config :out-dir))

  (println "Setup: Copying root media into out media")
  (fs/copy-dir (config :media-dir) (config :out-media-dir))

  (-> config layout/get-layouts-and-partials))

(defn parse!
  "Shells out to the rust binary to parse the org-mode file."
  [file-str]
  (let [res (sh/sh "./src/parser/target/debug/parser" file-str)]
    (if-not (= (res :exit) 0)
      (prn "Orgize failed to parse file." file-str res)
      (res :out))))

(defn get-files

  ;; TODO -  maybe better belongs in setup function?
  "Returns a list of files as Java objects. Filters out all non `.org` files."
  [{:keys [files-dir] :as config}]
  (println "Getting org files...")
  (conj config {:org-files (fs/find-files files-dir #"^.*\.(org)$")}))

(defn read-file
  "Pulls :curr-file from config > parses > put into config with new vals"
  [config]
  (let [file-orig   (-> config :curr-file :original)
        file-parsed (->> file-orig slurp parse!)
        file-name   (-> file-orig .getName (s/split #"\.") (first))]
    (config/update-curr-file config {:name file-name :as-json file-parsed})))

(defn dataify-file
  "Converts an org file into a bunch of data.
  TODO collect logbook, clock, for every file."
  [config]
  (let [file-json            (-> config :curr-file :as-json)
        file-edn             (-> file-json (json/parse-string true))
        file-keywords        (get-in file-edn [:children 0 :children])]
    (config/update-curr-file config {:as-edn file-edn :keywords file-keywords})))

(defn htmlify-file
  "Renders files according to their `layout` keyword."
  [config]
  (let [layout   (config/get-layout config)
        as-html  (layout/apply-template config layout)]

    (config/update-curr-file config {:as-html as-html})))

(defn write-file
  "Takes (file-)config input and writes html to output."
  [{:keys [out-dir curr-file]}]
  (let [curr-file-name (curr-file :name)
        out-file-name  (str out-dir curr-file-name ".html")
        out-html       (curr-file :as-html)]
    (println "Writing file: " curr-file-name)
    (spit out-file-name out-html)))

(defn -main
  "TODO:  Messy. move the `let` block into the `setup` fn"
  [& args]
  (let [files-dir          (first args)
        config             (config/default files-dir)
        config-with-layout (setup config) ;; side effectful
        org-files          (-> config get-files :org-files)]

    (doseq [f org-files]
      (->> f
           (config/set-curr-file config-with-layout)
           (read-file)
           (dataify-file)
           (htmlify-file)
           (write-file)))
    #_(if config/dev?
        (System/exit 0))))

;; (-main) ; I recommend not running this in your repl with many files. See test suite instead.
