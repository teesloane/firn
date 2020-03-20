(ns firn.util
  ;; (:refer-clojure :exclude [name parents])
  (:require [clojure.string :as s]
            [me.raynes.fs :as fs]))

(defn get-files-of-type
  "Takes an io/file sequence and gets all files of a specific extension."
  [fileseq ext]
  (filter (fn [f]
            (and
             (.isFile f)
             (-> f .getName (.endsWith ext))))
          fileseq))

(defn find-files-by-ext
  "Traverses a directory for all files of a specific extension."
  [dir ext]
  (let [ext-regex (re-pattern (str "^.*\\.(" ext ")$"))
        files     (fs/find-files dir ext-regex)]
    (if (= 0 (count files))
      (do (println "No" ext "files found at " dir) files)
      files)))


(defn file-name-no-ext
  "Removes an extension from a filename"
  [io-file]
  (-> io-file .getName (s/split #"\.") (first)))

(defn io-file->keyword
  "Turn a filename into a keyword."
  [io-file]
  (-> io-file file-name-no-ext keyword))

(defn file-list->key-file-map
  "Takes a list of files and returns a map of filenames as :keywords -> file"
  [file-list]
  (let [eval-file #(-> % .getPath slurp read-string eval)]
    (into {} (map #(hash-map (io-file->keyword %) (eval-file %)) file-list))))

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
