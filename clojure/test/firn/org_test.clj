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

(t/deftest get-headline-helper
  (t/testing "expected output"
    (let [sample-data
          {:type "headline",
           :level 1,
           :children
           [{:type "title", :level 1, :tags ["ATTACH"], :raw "Image Tests", :children [{:type "text", :value "Image Tests"}]}]}

          sample-data-with-multiple-children
          {:type "headline",
           :level 1,
           :children
           [{:type "title", :level 1, :raw "Headlines <2020-03-27 Fri>", :properties {:foo "bar"},
             :children [{:type "text", :value "Headlines "} {:type "timestamp", :timestamp_type "active", :start {:year 2020, :month 3, :day 27, :dayname "Fri"}}]}]}

          res1 (sut/get-headline-helper sample-data)
          res2 (sut/get-headline-helper sample-data-with-multiple-children)]

      (t/is (= res1 "Image Tests"))
      (t/is (= res2 "Headlines")))))


(t/deftest parsed-org-date->unix-time
  (t/testing "returns the expected value."
    (t/is (= 1585683360000
             (sut/parsed-org-date->unix-time (sample-logentry :start) (stub/gtf :tf-1 :processed))))))

(t/deftest logbook-year-stats
  (let [logbook        (-> (stub/gtf :tf-metadata :processed) :meta :logbook)
        res            (sut/logbook-year-stats logbook)
        first-of-2020  (first (get res 2020))
        second-of-2020 (second (get res 2020))]

    ;; the sample logbook has dates from 2017 and 2020.
    (t/is (contains? res 2020))
    (t/is (contains? res 2017))

    ;; there is no log entry for 2020-01-01 so we expect
    ;; to it be an unaltered map, as produced by build-year
    (t/is (= (first-of-2020 :log-sum) "00:00"))
    (t/is (= (first-of-2020 :log-count) 0))
    (t/is (= (-> first-of-2020 :logs-raw count) 0))
    (t/is (= (first-of-2020 :hour-sum) 0))

    ;; however, the second day, DOES have a log entry, (see file-metadata.org)
    (t/is (= (second-of-2020 :log-sum) "0:11"))
    (t/is (= (second-of-2020 :log-count) 1))
    (t/is (= (second-of-2020 :hour-sum) 0.18))
    (t/is (= (-> second-of-2020 :logs-raw count) 1))))
