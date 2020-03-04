(ns firn.core
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [cheshire.core :as json]
            [firn.markup :as m]
            [firn.util :as u]
            [clojure.string :as s]
            [hiccup.core :as h])
  (:gen-class))

(defn setup
  "Creates output directory for files and sets up starting
  config file that gets passed through all functions"
  [{:keys [files-dir out-dir] :as config}]
  (prn "Making _site output.")
  (.mkdir (java.io.File. out-dir))
  config)

(defn parse!
  "Shells out to the rust binary to parse the org-mode file."
  [file-str]
  (let [res (sh/sh "./src/parser/target/debug/parser" file-str)]
    (if-not (= (res :exit) 0)
      (prn "Failed to parse file.")
      (res :out))))

(defn get-files
  "Returns a list of files as Java objects. Filters out all non `.org` files."
  [{:keys [files-dir] :as config}]
  (println "Getting org files...")
  (let [site-dir       (io/file files-dir)
        site-dir-files (file-seq site-dir)
        org-files      (u/get-files-of-type site-dir-files ".org")]
    (cond
      (not (.isDirectory site-dir))
      (u/exit-with-err "No folder found at site-directory")

      (= (count org-files) 0)
      (u/exit-with-err "No .org files found in site-directory")

      :else
      (conj config {:org-files org-files}))))

(defn read-file
  "file (java object) > slurps > parses (rust) > config"
  [{:keys [curr-file] :as config}]
  (let [file-parsed (->> curr-file slurp parse!)]
    (merge
     config
     {:file-name (-> curr-file .getName (s/split #"\.") (first))
      :file-orig curr-file
      :file-json file-parsed})))

(defn dataify-file
  "Converts an org file into a bunch of data."
  [{:keys [file-name file-json] :as config}]
  (let [file-edn             (-> file-json (json/parse-string true))
        file-keywords        (get-in file-edn [:children 0 :children])
        file-edn-no-keywords (assoc-in file-edn [:children 0 :children] [])
        org-tree             (->> file-edn-no-keywords (tree-seq map? :children) (first))
        org->html            (m/template org-tree)]
    (conj
     config
     {:file-edn      file-edn
      :file-keywords file-keywords
      :org-tree      org-tree
      :out-html      org->html})))

(defn write-file
  "Takes (file-)config input and writes html to output."
  [config]
  (println "Writing files: " (config :file-name))
  (let [out-file-name (str (:out-dir config) (:file-name config) ".html")
        out-html      (:out-html config)]
    (spit out-file-name out-html)))

(defn -main
  [& args]
  (let [files-dir (first args)
        config    {:out-dir   (str files-dir "_site/")
                   :files-dir (first args)
                   :org-files nil
                   :curr-file nil}]

    (setup config)
    (doseq [f (:org-files (get-files config))]
      (-> (assoc config :curr-file f)
          (read-file)
          (dataify-file)
          (write-file)))
    (System/exit 0)))

;; (-main) ; I recommend not running this in your repl.
