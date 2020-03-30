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


(defn single-file-runner
  "Run the file conversion on a single file."
  []
  (fs/delete-dir (config-sample :dirname-out))
  (let [config (build/setup config-sample)]
    (build/single-file config f-2)))

(defn main-runner
  [dir-to-build]
  (let [config (build/prepare-config {:path dir-to-build})]
    (fs/delete-dir (config :dir-firn))
    (build/all-files {:path dir-to-build})))

;; (main-runner wiki-dir)
(main-runner test-dir)



#_(def sample
    (-> config-sample
       (config/set-curr-file-original f-2)
       (build/read-file)
       (build/dataify-file)
       (build/munge-file)
       (build/htmlify-file)))
