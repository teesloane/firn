(ns firn.core-test
  (:require [clojure.java.io :as io]
            [firn.config :as config]
            [firn.build :as build]
            [me.raynes.fs :as fs]))

(def test-dir      "test/firn/demo_org")
(def wiki-dir      "/Users/tees/Dropbox/wiki")
(def f-1           (io/file (str test-dir "/file1.org")))
(def f-2           (io/file (str test-dir "/file2.org")))
(def config-sample (config/default test-dir))


(defn main-runner-2
  [dir-to-build]
  (let [config (build/prepare-config {:path dir-to-build})]
    (fs/delete-dir (config :dir-firn))
    (build/all-files {:path dir-to-build})))

(main-runner-2 wiki-dir)
(main-runner-2 test-dir)

