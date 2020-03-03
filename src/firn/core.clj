(ns firn.core
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [cheshire.core :as json]
            [firn.orgformer :as orgformer]
            [clojure.string :as s])
  (:gen-class))


(defn parse!
  "Shells out to the rust binary to parse the org-mode file."
  [file-str]
  (sh/sh "./src/parser/target/debug/parser" file-str))

(defn get-files
  "Returns a list of files as Java objects.
  Filters out all non `.org` files.
  FIXME: handle empty folder; handle custom folder, handle none found."
  []
  (println "Getting org files...")
  (->> "/Users/tees/Dropbox/wiki/"
       clojure.java.io/file
       file-seq
       (filter #(.isFile %))
       (filter (fn [file] (-> file .getName (.endsWith ".org"))))))

(defn read-file
  "file (java object) > slurps > parses (rust) > to-edn > orgformer/trxer"
  [file]
  (println "Reading File..." (.getName file))
  (let [file-json (-> file slurp parse!)]
    (if-not (= (:exit file-json) 0)
      (prn "It failed -- FIXME handle this." file-json)
      (let [config {:file-edn      (-> file-json (:out) (json/parse-string true))
                    :file-name     (-> file .getName (s/split #"\.") (first))
                    :org-tree      {}
                    :file-keywords {}}
            new-config (orgformer/trxer config)]
        (orgformer/trxer config)))))

(defn write-file
  "Takes read and parsed content files and writes them to output."
  [config]
  (println "Writing files...")
  (let [out-file-name (str "tmp/" (:file-name config) ".html")
        out-html      (:out-html config)]
    (spit out-file-name out-html)))

(-> (get-files)
    (nth 1)
    (read-file)
    (write-file))

(defn -main
  [& args]
  (doseq [f (get-files)]
    (-> f
        (read-file)
        (write-file)))
  (System/exit 0))

;; (-main) ; I recommend not running this in your repl.
