(ns firn.core-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [firn.config :as config]
            [firn.build :as build]
            [me.raynes.fs :as fs]))


(def test-dir      "test/firn/demo_org")
(def f-1           (io/file (str test-dir "/file1.org")))
(def f-2           (io/file (str test-dir "/file2.org")))
(def config-sample (config/default test-dir))

;; (deftest _setup
;;   ;; TODO - test reading of layouts into memory.
;;   (let [out-dir       (config-sample :out-dir)
;;         out-media-dir (config-sample :out-media-dir)
;;         setup-res     (build/setup config-sample)]

;;     (testing "It creates _site output dir."
;;       (is (.isDirectory (io/file out-dir))))

;;     (testing "It creates a _site/media output dir."
;;       (is (.isDirectory (io/file out-media-dir))))

;;     (testing "Config returns unchanged"
;;       (is (= config-sample setup-res)))

;;     ;; cleanup
;;     (fs/delete-dir out-dir)))

;; (deftest _read-file
;;   (testing "correct outputs"
;;     (let [stub     (-> (build/setup config-sample)
;;                        (config/set-curr-file-original f-1))
;;           res      (build/read-file stub)
;;           res-json (-> res :curr-file :as-json)]
;;       (testing "Returns valid, updated sub map (:curr-file)"
;;         (is (not= nil (-> res :curr-file :name)))
;;         (is (not= nil res-json))
;;         (is (> (count res-json) 0))))))



(defn single-file-runner
  []
  (fs/delete-dir (config-sample :out-dirname)) ;; clear it out!
  (let [config (build/setup config-sample)]
    (build/single-file config f-2)))

(-> (single-file-runner))

(defn main-runner
  []
  (fs/delete-dir (config-sample :out-dirpath)) ; clear it out!
  (build/all-files {; :path test-dir
                    :path "/Users/tees/Dropbox/wiki"}))
                     ; delete folder if it exists


(main-runner)
