(ns firn.stubs
  (:require [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [firn.config :as config]))



(def test-dir    "test/firn/demo_org")
(def firn-dir    (str test-dir "/_firn"))
(def test-file-1 (io/file "test/firn/demo_org/file1.org"))

(defn delete-firn-dir
  []
  (fs/delete-dir firn-dir))

(defn make-dummy-config
  []
  (config/prepare test-dir))
