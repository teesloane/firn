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
            [firn.file :as file]
            [firn.build :as build]))

(def test-dir    "test/firn/demo_org")
(def firn-dir    (str test-dir "/_firn"))

(defn delete-firn-dir
  []
  (fs/delete-dir firn-dir))

(defn sample-config
  []
  (build/new-site {:dir-files test-dir})
  (config/prepare test-dir))


;; TODO: This should all get abstracted into a map.


(def test-file-1                       (io/file "test/firn/demo_org/file1.org"))
;; (def test-file-no-keywords             (io/file "test/firn/demo_org/file-no-keywords.org")) ;; FIXME: this needs to be tested against the binary output.
(def test-file-small                   (io/file "test/firn/demo_org/file-small.org"))
(def test-file-private                 (io/file "test/firn/demo_org/file-private.org"))
(def test-file-private-subfolder       (io/file "test/firn/demo_org/file-private.org"))
(def test-file-metadata                 (io/file "test/firn/demo_org/file-metadata.org"))
;; Processed test files.
(def test-file-1-processed             (file/process-one (sample-config) test-file-1))
;; (def test-file-no-keywords-processed   (file/process-one (sample-config) test-file-no-keywords))
(def test-file-small-processed         (file/process-one (sample-config) test-file-1))
(def test-file-private-processed       (file/process-one (sample-config) test-file-private))
(def test-file-private-subfolder-processed       (file/process-one (sample-config) test-file-private))
(def test-file-metadata-processed                 (file/process-one (sample-config) test-file-metadata))


;; fixtures


(defn test-wrapper
  [f-test]
  (println "Running test...")
  (delete-firn-dir)
  (f-test))
