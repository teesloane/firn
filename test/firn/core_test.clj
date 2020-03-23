(ns firn.core-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [firn.config :as config]
            [firn.build :as build]
            [me.raynes.fs :as fs]))

(def test-dir      "test/firn/demo_org")
(def wiki-dir      "/Users/tees/Dropbox/wiki")
(def f-1           (io/file (str test-dir "/file1.org")))
(def f-2           (io/file (str test-dir "/file2.org")))
(def config-sample (config/default test-dir))


(defn single-file-runner
  []
  (fs/delete-dir (config-sample :out-dirname)) ;; clear it out!
  (let [config (build/setup config-sample)]
    (build/single-file config f-2)))

;; (-> (single-file-runner))

(defn main-runner
  [dir-to-build]
  (fs/delete-dir (config-sample :out-dirpath)) ; clear it out!
  (build/all-files {:path dir-to-build}))

(main-runner wiki-dir)
;; (main-runner test-dir)


(build/new-site {:path wiki-dir})
