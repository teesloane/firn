(ns firn.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [firn.core]
            [firn.config :as config]))


(def test-dir      "./tmp/")
(def f-1           (io/file "./test/firn/sample_orgfile.org"))
(def config-sample (config/default test-dir))


(deftest _setup
  (let [out-dir (config-sample :out-dir)
        setup-res (firn.core/setup config-sample)]

    (testing "It creates a folder"
      (is (.isDirectory (io/file out-dir))))

    (testing "Config returns unchanged"
      (is (= config-sample setup-res)))))



(deftest _read-file ;; (testing "correct outputs"
  (let [stub     (-> (firn.core/setup config-sample)
                     (config/set-curr-file f-1))
        res      (firn.core/read-file stub)
        res-json (-> res :curr-file :as-json)]
    (testing "Returns valid, updated sub map (:curr-file)"
      (is (not= nil (-> res :curr-file :name)))
      (is (not= nil res-json))
      (is (> (count res-json) 0)))))



(defn e2e
  []
  (firn.core/setup config-sample)
  (->> f-1
       (config/set-curr-file config-sample)
       (read-file)
       (dataify-file)
       (write-file)))

(e2e)
