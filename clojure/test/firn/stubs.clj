(ns firn.stubs
  "Common files and data representations of files for tests.
  Since Firn moves a data structure (config/file) through pipelines, testing is
  still a bit loosely defined. For now, we process a series of files with
  different attributes and test expected output from functions (even though the
  data has probably run through several functions up to that point.)

  When we can, we pass the minimal needed inputs for unit tests, but for the
  rest, we have the stubs in this namespace.
  "
  (:require [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [firn.config :as config]
            [clojure.test]
            [firn.build :as build]))

(def test-dir    "test/firn/demo_org")
(def firn-dir    (str test-dir "/_firn"))

(defmethod clojure.test/report :begin-test-var [m]
  (println "Running test: " (-> m :var meta :name)))

(defn delete-firn-dir
  []
  (fs/delete-dir firn-dir))

(defn sample-config
  []
  (delete-firn-dir)
  (build/new-site {:dir test-dir})
  (config/prepare {:dir test-dir}))


;; org test files that we can request using "get-test-file"


(def test-files
  {:tf-1                 "file1.org"
   :tf-small             "file-small.org"
   :tf-private           "file-private.org"
   :tf-private-subfolder "priv/file1.org"
   :tf-layout            "file-layout.org"
   :tf-footnotes         "file-footnotes.org"
   :tf-metadata          "file-metadata.org"
   :tf-underscores       "file_underscores.org"})

(defn gtf ; stands for get-test-file
  "Gets a test org file, and can return it as an io object or a processed file."
  [tf res-form]
  (let [file-path (str "test/firn/demo_org/" (get test-files tf))
        prep-config (sample-config)]
    (case res-form
      :path      file-path
      :io        (io/file file-path)
      :processed (->> file-path io/file (build/process-one prep-config)))))

;; fixtures

(defn test-wrapper
  [f-test]
  (delete-firn-dir)
  (f-test))
