(ns fc-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [firn.core :as fc]
            [firn.config :as config]))


(def test-dir      "./tmp/")
(def f-1           (io/file "./test/firn/sample_orgfile.org"))
(def config-sample (config/default test-dir))


(deftest _setup
  (let [out-dir (config-sample :out-dir)
        setup-res (fc/setup config-sample)]

    (testing "It creates a folder"
      (is (.isDirectory (io/file out-dir))))

    (testing "Config returns unchanged"
      (is (= config-sample setup-res)))))



(deftest _read-file ;; (testing "correct outputs"
  (let [stub     (-> (fc/setup config-sample)
                     (config/set-curr-file f-1))
        res      (fc/read-file stub)
        res-json (-> res :curr-file :as-json)]
    (testing "Returns valid, updated sub map (:curr-file)"
      (is (not= nil (-> res :curr-file :name)))
      (is (not= nil res-json))
      (is (> (count res-json) 0)))))



(defn e2e
  []
  (fc/setup config-sample)
  (->> f-1
       (config/set-curr-file config-sample)
       (fc/read-file)
       (fc/dataify-file)

       #_(fc/write-file)))

(e2e)
