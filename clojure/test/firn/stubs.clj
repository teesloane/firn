(ns firn.stubs
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

(defn make-dummy-config
  []
  (build/new-site {:dir-files test-dir})
  (config/prepare test-dir))


(def test-file-1           (io/file "test/firn/demo_org/file1.org"))
(def test-file-no-keywords (io/file "test/firn/demo_org/file-no-keywords.org"))
(def test-file-small       (io/file "test/firn/demo_org/file-small.org"))
;; Processed test files.
(def test-file-1-processed             (file/process-one (make-dummy-config) test-file-1))
(def test-file-no-keywords-processed   (file/process-one (make-dummy-config) test-file-no-keywords))
(def test-file-small-processed         (file/process-one (make-dummy-config) test-file-1))



;; fixtures

(defn test-wrapper
  [f-test]
  (println "Running test...")
  (delete-firn-dir)
  (f-test))
