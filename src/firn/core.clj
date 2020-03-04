(ns firn.core
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [cheshire.core :as json]
            [firn.orgformer :as orgformer]
            [firn.markup :as m]
            [firn.util :as u]
            [clojure.string :as s]
            [hiccup.core :as h])
  (:gen-class))


(defn parse!
  "Shells out to the rust binary to parse the org-mode file."
  [file-str]
  (sh/sh "./src/parser/target/debug/parser" file-str))

(defn get-files
  "Returns a list of files as Java objects.
  Filters out all non `.org` files.
  FIXME: handle custom folder,"
  [file-dir]
  (println "Getting org files...")
  (let [site-dir   (io/file file-dir)
        site-files (file-seq site-dir)
        org-files  (u/get-files-of-type site-files ".org")]
    (cond
      (not (.isDirectory site-dir)) ;; when file-dir
      (u/exit-with-err "No folder found at site-directory")

      (= (count org-files) 0)
      (u/exit-with-err "No .org files found in site-directory")

      :else org-files)))



(defn read-file
  "file (java object) > slurps > parses (rust) > to-edn > orgformer/trxer"
  [file]
  (println "Reading File..." (.getName file))
  (let [file-json (-> file slurp parse!)]
    (if-not (= (:exit file-json) 0)
      (prn "It failed -- FIXME handle this." file-json)
      {:file-name (-> file .getName (s/split #"\.") (first))
       :file-orig file
       :file-json file-json})))

(defn dataify-file
  "Converts an org file into a bunch of data."
  [{:keys [file-name file-json] :as config}]
  (let [file-edn             (-> file-json (:out) (json/parse-string true))
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
  "Takes read and parsed content files and writes them to output."
  [config]
  (println "Writing files...")
  (let [out-file-name (str "tmp/" (:file-name config) ".html")
        out-html      (:out-html config)]
    (spit out-file-name out-html)))

;; (-> (get-files)
;;     (nth 4)
;;     (read-file)
;;     (write-file))


(defn -main
  [& args]
  (prn args)

  (doseq [f (get-files (first args))]
    (-> f
        (read-file)
        (dataify-file)
        (write-file)))
  (System/exit 0))

;; (-main) ; I recommend not running this in your repl.
