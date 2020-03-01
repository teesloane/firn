(ns firn.util
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [sci.core :as sci]))

;; Some of these are borrowed from me.raynes.fs because I need to add ;; type hints for GraalVM

(def dev? (if (= (System/getenv "DEV") "TRUE") true false))

(defn print-err!
  "A custom error function.
  Prints errors, expecting a type to specified (:warning, :error etc.)
  Currently, also returns false after printing error message, so we can
  use that for control flow or for tests."
  [typ & args]
  (let [err-types   {:warning       "🚧 Warning:"
                     :error         "❗ Error:"
                     :uncategorized "🗒 Uncategorized Error:"}
        sel-log-typ (get err-types typ (get err-types :uncategorized))]
    (apply println sel-log-typ args)
    (when (and (not dev?) (= typ :error))
      (System/exit 1))))

(def ^{:doc "Current working directory. This cannot be changed in the JVM.
             Changing this will only change the working directory for functions
             in this library."
       :dynamic true}
  *cwd* (.getCanonicalFile (io/file ".")))

(defn ^java.io.File file
  "If `path` is a period, replaces it with cwd and creates a new File object
   out of it and `paths`. Or, if the resulting File object does not constitute
   an absolute path, makes it absolutely by creating a new File object out of
   the `paths` and cwd."
  [path & paths]
  (when-let [path (apply io/file (if (= path ".") *cwd* path) paths)]
    (if (.isAbsolute ^java.io.File path)
      path
      (io/file *cwd* path))))

(defn find-files*
  "Find files in `path` by `pred`."
  [path pred]
  (filter pred (-> path file file-seq)))

(defn find-files
  "Find files matching given `pattern`."
  [path pattern]
  (find-files* path #(re-matches pattern (.getName ^java.io.File %))))

(defn find-files-by-ext
  "Traverses a directory for all files of a specific extension."
  [dir ext]
  (let [ext-regex (re-pattern (str "^.*\\.(" ext ")$"))
        files     (find-files dir ext-regex)]
    (if (= 0 (count files))
      (do (print-err! :warning "No" ext "files found at " dir) files)
      files)))

(defn file-name-no-ext
  "Removes an extension from a filename"
  [io-file]
  (let [f (.getName ^java.io.File io-file)]
    (-> f (s/split #"\.") (first))))

(defn get-cwd
  "Because *fs/cwd* gives out the at-the-time jvm path. this works with graal."
  []
  (s/join "/" (-> (java.io.File. ".")
                  .getAbsolutePath
                  (s/split #"/")
                  drop-last)))

(defn io-file->keyword
  "Turn a filename into a keyword."
  [io-file]
  (-> io-file file-name-no-ext keyword))

;; File Path fns ----
;; Mostly for operating on paths: `file/paths/woo/hoo.org`

(defn remove-ext
  "removes an extension from a string.
  Optionally, you can specify to only do so for specified extensions."
  ([s]
   (-> s (s/split #"\.") first))
  ([s ext]
   (let [split (s/split s #"\.")
         filename (first split)
         -ext (last split)]
     (if (= ext -ext) filename s))))

(defn read-and-eval-clj
  [io-file]
  (let [file-path (.getPath ^java.io.File io-file)
        eval-file (-> file-path slurp sci/eval-string)]
    eval-file))

(defn load-fns-into-map
  "Takes a list of files and returns a map of filenames as :keywords -> file
  NOTE: It also EVALS (using sci) the files so they are in memory functions!
 
  so:                  `[my-file.clj my-layout.clj]`
  ------------------------------- ▼ ▼ ▼ ----------------------------------------
  becomes:    {:my-file fn-evald-1, :my-layout fn-evald-2}"

  [file-list]
  (let [file-path #(.getPath ^java.io.File %)
        eval-file #(-> % file-path slurp sci/eval-string)]
    (into {} (map #(hash-map (io-file->keyword %) (eval-file %)) file-list))))

(defn dupe-name-in-dir-path?
  "Takes a str path of a directory and checks if a folder name appears more than
  once in the path"
  [dir-path dir-name]
  (> (get (frequencies (s/split dir-path #"/")) dir-name) 1))

(defn get-differing-path
  "compares two paths; returns `path-b` when it diverges from matching path-a
  ex: (get-differing-path `x/y/bar/jo` `x/y/bar/jo/bru/brunt`) => /bru/brunt"
  [path-a path-b]
  (let [split-a (s/split path-a #"/")
        split-b (s/split path-b #"/")]
    (loop [list-a split-a
           list-b split-b]
      (if-not (= (first list-a) (first list-b))
        (s/join "/" list-b)
        (recur (rest list-a) (rest list-b))))))

;; General fns ----

(defn find-first
  "Find the first item in a collection."
  [f coll]
  (first (filter f coll)))

;; For interception thread macros and enabling printing the passed in value.
(def spy #(do (println "DEBUG:" %) %))



(defn native-image?
  "Check if we are in the native-image or REPL."
  []
  (and (= "Substrate VM" (System/getProperty "java.vm.name"))
       (= "runtime" (System/getProperty "org.graalvm.nativeimage.imagecode"))))

(defn str->keywrd
  "Converts a string to a keyword"
  [& args]
  (keyword (apply str args)))
