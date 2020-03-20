(ns firn.util
  ;; (:refer-clojure :exclude [name parents])
  (:require [clojure.string :as s]))

(defn get-files-of-type
  "Takes an io/file sequence and gets all files of a specific extension."
  [fileseq ext]
  (filter (fn [f]
            (and
             (.isFile f)
             (-> f .getName (.endsWith ext))))
          fileseq))

(defn file-name-no-ext
  "Removes an extension from a filename"
  [io-file]
  (-> io-file .getName (s/split #"\.") (first)))

(defn io-file->keyword
  "Turn a filename into a keyword."
  [io-file]
  (-> io-file file-name-no-ext keyword))

(defn exit-with-err
  "Exits with error.
  TODO: make this not exit the repl in dev-mode."
  [& msgs]
  (prn "Err: " msgs)
  #_(System/exit 1))

(defn find-first
  [f coll]
  (first (filter f coll)))

(def spy #(do (println "DEBUG:" %) %))
