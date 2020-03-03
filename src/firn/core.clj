(ns firn.core
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [cheshire.core :as json]
            [firn.extracter :as ex])
  (:gen-class))


;; Files Reading
;; talk to rust binary!
(defn parse!
  [file-str]
  (sh/sh "./src/parser/target/debug/parser" file-str))


(defn get-files
  "Returns a list of files as Java objects.
  Filters out all non `.org` files.
  FIXME: handle empty folder; handle custom folder, handle none found.
  "
  []
  (println "Getting org files...")
  (->> "/Users/tees/Dropbox/wiki/"
       clojure.java.io/file
       file-seq
       (filter #(.isFile %))
       (filter (fn [file] (-> file .getName (.endsWith ".org"))))))


(defn read-file
  "Takes a list of files and reads them, eventually parsing them."
  [file]
  (println "Reading File..." (.getName file))
  (let [f (-> file slurp parse!)]
    (if-not (= (:exit f) 0)
      (prn "It failed -- handle this.")
      (-> f
          (:out)
          (json/parse-string true)
          (ex/get-meta)
          #_(ex/trxer)))))

         





(defn write-file
  "Takes read and parsed content files and writes them to output."
  [f]
  (println "Writing files...")
  (spit "out.edn" (pr-str f))
  f)


(-> (get-files)
    (first)
    (read-file))


;; File Writing

(defn -main
  [& args]
  (-> (get-files)
      (first)
      (read-file)
      (write-file)))


(-main)
