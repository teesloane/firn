(ns firn.core
  (:require [cheshire.core :as json]
            [clojure.java.shell :as sh]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [firn.config :as config]
            [firn.layout :as layout]
            [me.raynes.fs :as fs]
            [firn.util :as u])
  (:gen-class))

(defn- build-file-outpath
  "For the current file, build it's output filename
  based on out-dir, the path of the file (it could be several layers deep)
  Returns the file name as a string."
  [{:keys [out-dirname files-dirname curr-file]}]
  (let [curr-file-path (-> curr-file :original .getPath)
        out-comb       (str files-dirname "/" out-dirname)]
    (-> curr-file-path
        (s/replace #"\.org" ".html")
        (s/replace (re-pattern files-dirname) (str out-comb))))) ;; < str to make linter happy.


;; Le grandiose --------------------------------

(defn setup
  "Creates folders for output, slurps in layouts and partials.
  FIXME: should slurp/mkdir/copy-dir be wrapped in try-catches? if-err handling?"
  [{:keys [layouts-dir partials-dir files-dir] :as config}]
  (let [layout-files  (u/find-files-by-ext layouts-dir "clj")
        partial-files (u/find-files-by-ext partials-dir "clj")
        partials-map  (u/file-list->key-file-map partial-files)
        layouts-map   (u/file-list->key-file-map layout-files)]


    (println "Setup: Making _site output.")
    (fs/mkdir (config :out-dir))

    (println "Setup: Copying root media into out media")
    (fs/copy-dir (config :media-dir) (config :out-media-dir))

    (assoc config
           :org-files (u/find-files-by-ext files-dir "org")
           :layouts layouts-map
           :partials partials-map)))

(defn parse!
  "Shells out to the rust binary to parse the org-mode file."
  [file-str]
  (let [res (sh/sh "./src/parser/target/debug/parser" file-str)]
    (if-not (= (res :exit) 0)
      (prn "Orgize failed to parse file." file-str res)
      (res :out))))

(defn read-file
  "Pulls :curr-file from config > parses > put into config with new vals"
  [config]
  (let [file-orig   (-> config :curr-file :original)
        file-parsed (->> file-orig slurp parse!)
        file-name   (-> file-orig .getName (s/split #"\.") (first))]
    (config/update-curr-file config {:name file-name :as-json file-parsed})))

(defn dataify-file
  "Converts an org file into a bunch of data."
  [config]
  (let [file-json (-> config :curr-file :as-json)
        file-edn  (-> file-json (json/parse-string true))]
    (config/update-curr-file config {:as-edn file-edn})))

(defn munge-file
  "After dataify-file,  we extract information and store it in curr-file."
  [config]
  (config/update-curr-file
   config
   {:keywords    (config/get-keywords config)
    :org-title   (config/get-keyword config "TITLE")}))

(defn htmlify-file
  "Renders files according to their `layout` keyword."
  [config]
  (let [layout   (keyword (config/get-keyword config "LAYOUT"))
        as-html  (when-not (config/file-is-private? config)
                   (layout/apply-template config layout))]

    (config/update-curr-file config {:as-html as-html})))

(defn write-file
  "Takes (file-)config input and writes html to output."
  [{:keys [curr-file] :as config}]
  (let [curr-file-name (curr-file :name)
        out-file-name  (build-file-outpath config)
        out-html       (curr-file :as-html)]
    (println "Writing file: " curr-file-name "to " out-file-name)
    (when-not (config/file-is-private? config)
      (io/make-parents out-file-name)
      (spit out-file-name out-html))))

(defn -main
  "Takes a link to a directory and runs on the list of org files."
  [& args]
  (let [files-dir          (first args) ;; TODO setup cli args.
        config             (-> files-dir config/default setup)]

    (doseq [f (config :org-files)]
      (->> f
         (config/set-curr-file-original config)
         (read-file)
         (dataify-file)
         (munge-file)
         (htmlify-file)
         (write-file)))
    #_(if config/dev?
        (System/exit 0))))

;; (-main) ; I recommend not running this in your repl with many files. See test suite instead.
