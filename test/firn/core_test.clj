(ns firn.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [firn.core]
            [firn.config :as config]))


(def test-dir    "./tmp/")
(def f-1         (io/file "./test/firn/sample_orgfile.org"))
(def f-1-str     (slurp f-1))
(def config-test (config/default test-dir))


(deftest _setup
  (let [out-dir (config-test :out-dir)
        setup-res (firn.core/setup config-test)]

    (testing "It creates a folder"
      (is (.isDirectory (io/file out-dir))))

    (testing "Config returns unchanged"
      (is (= config-test setup-res)))))



;; (deftest _read-file
  ;; (testing "correct outputs"
  ;;   (let [stub (-> (setup-files))
  ;;         res (firn.core/read-file setup-res)]
  ;;     (prn "RES IS " res)
  ;;     (is (= 1 1)))))
      ;; (let [res (firn.core/read-file setup-res)]
      ;;   (is (=  1 1))))))


;; "E2E"
