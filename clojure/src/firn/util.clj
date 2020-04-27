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

(defn native-image?
  "Check if we are in the native-image or REPL."
  []
  (and (= "Substrate VM" (System/getProperty "java.vm.name"))
       (= "runtime" (System/getProperty "org.graalvm.nativeimage.imagecode"))))

(defn print-err!
  "A custom error function.
  Prints errors, expecting a type to specified (:warning, :error etc.)
  Currently, also returns false after printing error message, so we can
  use that for control flow or for tests.
  TODO: read up on error testing and how to best handle these things.
  "
  [typ & args]
  (let [err-types   {:warning       "🚧 Warning:"
                     :error         "❗ Error:"
                     :uncategorized "🗒 Uncategorized Error:"}
        sel-log-typ (get err-types typ (get err-types :uncategorized))]
    (apply println sel-log-typ args)
    false))

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
  NOTE: It also EVALS (using sci) the files so they are in memory functions!
  FIXME: You should probably rename this file because it doens't JUST
  map keys, it evals stuff."
  [file-list]
  (let [file-path #(.getPath ^java.io.File %)
        eval-file #(-> % file-path slurp sci/eval-string)]
    (into {} (map #(hash-map (io-file->keyword %) (eval-file %)) file-list))))

(defn find-first
  "Find the first item in a collection."
  [f coll]
  (first (filter f coll)))

(defn get-cwd
  "Because *fs/cwd* gives out the at-the-time jvm path. this works with graal."
  []
  (s/join "/" (-> (java.io.File. ".")
                  .getAbsolutePath
                  (s/split #"/")
                  drop-last)))

(defn dupe-name-in-dir-path?
  "Takes a str path of a directory and checks if a folder name appears more than
  once in the path"
  [dir-path dir-name]
  (> (get (frequencies (s/split dir-path #"/")) dir-name) 1))

(def spy #(do (println "DEBUG:" %) %))
