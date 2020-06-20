(ns firn.layout-test
  (:require [firn.layout :as sut]
            [firn.stubs :as stub]
            [clojure.test :as t]
            [firn.org :as org]
            [firn.file :as file]
            [firn.build :as build]))

(t/deftest prepare
  (let [test-file     (stub/gtf :tf-1 :processed)
        sample-config (stub/sample-config)
        res           (sut/prepare sample-config test-file)]
    (t/is (every? #(contains? res %) [:render :title :site-map :site-links :site-logs :meta :partials :config]))))

(t/deftest get-layout
  (t/testing "The tf-layout file returns a sci function.")
  (let [test-file     (stub/gtf :tf-layout :processed)
        sample-config (build/setup (stub/sample-config))
        layout        (keyword (file/get-keyword test-file "FIRN_LAYOUT"))
        res           (sut/get-layout sample-config test-file layout)]
    (t/is (= sci.impl.vars.SciVar (type res)))))
