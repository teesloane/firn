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

;; TODO - test that this throws/prints an error when an incorrectly formatted timestamp.
;; ie; missing end-date.
(t/deftest parsed-org-date->unix-time
  (t/testing "returns the expected value."
    (t/is (= 1585683360000
             (sut/parsed-org-date->unix-time (sample-logentry :start) (stub/gtf :tf-1 :processed))))))

(t/deftest logbook-year-stats
  (let [logbook        (-> (stub/gtf :tf-metadata :processed) :meta :logbook)
        res            (sut/logbook-year-stats logbook)
        first-of-2020  (first (get res 2020))
        second-of-2020 (second (get res 2020))]

    (prn "second" second-of-2020)
    ;; the sample logbook has dates from 2017 and 2020.
    (t/is (contains? res 2020))
    (t/is (contains? res 2017))
    ;; there is no log entry for 2020-01-01 so we expect
    ;; to it be an unaltered map, as produced by build-year
    (t/is (= first-of-2020 {:date #time/date "2020-01-01", :log-sum "00:00", :log-count 0, :logs-raw [], :hour-sum 0}))
    ;; however, the second day, DOES have a log entry, (see file-metadata.org)
    (t/is (= second-of-2020 {:date #time/date "2020-01-02",
                             :log-sum "0:11",
                             :log-count 1,
                             :logs-raw [{:end-ts 1577982000000, :start {:year 2020, :month 1, :day 2, :dayname "Thu", :hour 16, :minute 9}, :start-ts 1577981340000, :type "clock", :from-file-path "file-metadata", :duration "0:11", :from-headline "A headline with a normal log-book.", :post_blank 0, :end {:year 2020, :month 1, :day 2, :dayname "Thu", :hour 16, :minute 20}, :from-file "file-metadata"}],
                             :hour-sum 0.18}))))
