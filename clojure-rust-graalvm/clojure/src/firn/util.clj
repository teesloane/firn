(ns firn.util
  (:require [clojure.string :as s]
            ;; vendored me.raynes.fs because it needed type hint changes to compiled with graal
            [firn.fs :as fss]
            [sci.core :as sci]))
 
(defn get-files-of-type
  "Takes an io/file sequence and gets all files of a specific extension."
  [fileseq ext]
  (filter (fn [f]
            (let [is-file        (.isFile ^java.io.File f)
                  file-name      (.getName ^java.io.File f)
                  file-ends-with (s/ends-with? file-name ext)]
              (and is-file file-ends-with)))
          fileseq))

(defn print-err!
  "A semantic error function."
  [& args]
  (apply println args))

(defn str->keywrd
  [& args]
  (keyword (apply str args)))

(defn find-files-by-ext
  "Traverses a directory for all files of a specific extension."
  [dir ext]
  (let [ext-regex (re-pattern (str "^.*\\.(" ext ")$"))
        files     (fss/find-files dir ext-regex)]
    (if (= 0 (count files))
      (do (println "No" ext "files found at " dir) files)
      files)))

(defn file-name-no-ext
  "Removes an extension from a filename"
  [io-file]
  (let [f (.getName ^java.io.File io-file)]
    (-> f (s/split #"\.") (first))))

(defn io-file->keyword
  "Turn a filename into a keyword."
  [io-file]
  (-> io-file file-name-no-ext keyword))

(defn file-list->key-file-map
  "Takes a list of files and returns a map of filenames as :keywords -> file
  NOTE: It also EVALS the files so they are in memory functions!
  NOTE: You should probably rename this file because it doens't JUST
  map keys, it evals stuff."
  [file-list]
  (let [file-path #(.getPath ^java.io.File %)
        eval-file #(-> % file-path slurp sci/eval-string)]
    (into {} (map #(hash-map (io-file->keyword %) (eval-file %)) file-list))))

(defn find-first
  "Find the first item in a collection."
  [f coll]
  (first (filter f coll)))

(def spy #(do (println "DEBUG:" %) %))
