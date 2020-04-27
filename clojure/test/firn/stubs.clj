(ns firn.stubs
  "Common files and data representations of files for tests."
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
(def test-file-no-keywords             (io/file "test/firn/demo_org/file-no-keywords.org"))
(def test-file-small                   (io/file "test/firn/demo_org/file-small.org"))
(def test-file-private                 (io/file "test/firn/demo_org/file-private.org"))
(def test-file-private-subfolder       (io/file "test/firn/demo_org/file-private.org"))
;; Processed test files.
(def test-file-1-processed             (file/process-one (sample-config) test-file-1))
(def test-file-no-keywords-processed   (file/process-one (sample-config) test-file-no-keywords))
(def test-file-small-processed         (file/process-one (sample-config) test-file-1))
(def test-file-private-processed       (file/process-one (sample-config) test-file-private))
(def test-file-private-subfolder-processed       (file/process-one (sample-config) test-file-private))



;; fixtures

(defn test-wrapper
  [f-test]
  (println "Running test...")
  (delete-firn-dir)
  (f-test))
