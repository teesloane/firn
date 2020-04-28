(ns firn.org-test
  (:require [firn.org :as sut]
            [clojure.test :as t]
            [firn.stubs :as stub]))

(def sample-logentry
  {:type "clock", :start {:year 2020, :month 3, :day 31, :dayname "Tue", :hour 19, :minute 36}, :end {:year 2020, :month 3, :day 31, :dayname "Tue", :hour 19, :minute 46}, :duration "0:10", :post_blank 0})


(t/deftest parse!
  (let [res (sut/parse! "* A headline")]
    (t/is (= res "{\"type\":\"document\",\"pre_blank\":0,\"children\":[{\"type\":\"headline\",\"level\":1,\"children\":[{\"type\":\"title\",\"level\":1,\"raw\":\"A headline\",\"post_blank\":0,\"children\":[{\"type\":\"text\",\"value\":\"A headline\"}]}]}]}\n"))))

(t/deftest get-headline
  (let [file (stub/gtf :tf-1 :processed)
        res  (sut/get-headline (file :as-edn) "A heading with a line of content")]
    (t/testing "It returns the expected value."
      (t/is (= (res :type) "headline"))
      (t/is (> (count (res :children)) 0)))))


(t/deftest get-headline-content
  (let [file (stub/gtf :tf-1 :processed)
        res  (sut/get-headline-content (file :as-edn) "A heading with a line of content")]
    (t/testing "It returns the expected value."
      (t/is (= (-> res :children first :type) "section")))))

(t/deftest parsed-org-date->unix-time
  (t/testing "returns the expected value."
    (t/is (= 1585683360000 (sut/parsed-org-date->unix-time (sample-logentry :start))))))
