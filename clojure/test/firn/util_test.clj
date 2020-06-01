(ns firn.util-test
  (:require [firn.util :as sut]
            [firn.stubs :as stub]
            [clojure.test :as t]
            [me.raynes.fs :as fs]))

(def num-testing-files 9)

(t/deftest dupe-name-in-dir-path?
  (t/testing "Returns true on a path that has a duplicate dir"
    (t/is (= true (sut/dupe-name-in-dir-path?  "/Users/foo/bar/my-dir/some-files/my-dir/foo" "my-dir")))
    (t/is (= false (sut/dupe-name-in-dir-path?  "/Users/some-files/my-dir/foo/bar" "my-dir")))))

(t/deftest find-first
  (t/testing "It returns the first item of a vec"
    (t/is (= {:foo "bar", :baz "jo"}
             (sut/find-first #(= (% :foo) "bar") [{:foo "bar" :baz "jo"} {:foo "boo" :cluck "chicken"}])))))

(t/deftest str->keywrd
  (t/testing "It does the job."
    (t/is (= :foo (sut/str->keywrd "foo")))
    (t/is (= :foo.boo (sut/str->keywrd "foo" "." "boo")))))

(t/deftest file-name-no-ext
  (let [file (stub/gtf :tf-1 :io)]
    (t/testing "it removes the extension from the file name."
      (t/is (= "file1" (sut/file-name-no-ext file))))))

(t/deftest load-fns-into-map
  (let [config       (stub/sample-config)
        layout-files (sut/find-files-by-ext (config :dir-layouts) "clj")
        res         (sut/load-fns-into-map layout-files)]
    (t/testing "It loads some functions"
      (t/is (= true (contains? res :default)))
      ;; the file loaded should be a sci-eval'd function.
      (t/is (= sci.impl.vars.SciVar (type (res :default)))))))

(t/deftest io-file->keyword
  (let [res  (sut/io-file->keyword (stub/gtf :tf-1 :io))
        res2 (sut/io-file->keyword (stub/gtf :tf-underscores :io))]
    (t/testing "a file name becomes a keyword"
      (t/is (= :file1 res))
      (t/is (= :file-underscores res2)))))

(t/deftest find-files-by-ext
  (t/testing "It finds the demo_org org files."
    (let [files (sut/find-files-by-ext stub/test-dir "org")]
      (t/is (= num-testing-files (count files))) ; not the best test, but only has to be updated when number of org sample files change.
      (doseq [f files]
        (t/is (= ".org" (fs/extension (.getPath f)))))))
  (t/testing "It returns an empty list when nothing is found"
    (let [no-files (sut/find-files-by-ext stub/test-dir "foo")]
      (t/is (= (count no-files) 0)))))

(t/deftest remove-ext
  (t/testing "It removes the extension from a string."
    (t/is (= "foo" (sut/remove-ext "foo.org"))))

  (t/testing "It removes a specified extension"
    (t/is (= "foo" (sut/remove-ext "foo.html" "html"))))

  (t/testing "It does not remove the extension if the specified extension does not match"
    (t/is (= "foo.html" (sut/remove-ext "foo.html" "org")))))

(t/deftest find-index-of
  (t/testing "returns expected output."
    (let [test-seq1 [1 2 3 4 5 6 7]
          res1      (sut/find-index-of #(= % 3) test-seq1)
          test-seq2 [{:foo "bar" :baz 30} {:foo "non" :baz 10 :x "30"} {:baz 60}]
          res2 (sut/find-index-of #(> (% :baz) 20) test-seq2)]
      (t/is (= res1 2))
      (t/is (= res2 0)))))

(t/deftest snake->kebab
  (t/testing "expected outpout"
    (let [res1 (sut/snake->kebab "foo_bar")
          res2 (sut/snake->kebab "foo_bar foo" :key-it)]
      (t/is (= res1 "foo-bar"))
      (t/is (= res2 :foo-bar-foo)))))

(t/deftest prepend-vec
  (t/testing "expected output."
    (let [res (sut/prepend-vec 1 [2 3])]
      (t/is (= res [1 2 3])))))

;; -- Time / Date Tests --------------------------------------------------------

(t/deftest timestr->hours-min
  (t/testing "It returns expected output"
    (let [res1 (sut/timestr->hours-min "1:36")
          res2 (sut/timestr->hours-min "13:06")]
      (t/is (= res1 [1 36]))
      (t/is (= res2 [13 06])))))

(t/deftest timevec->time-str
  (t/testing "It returns expected output"
    (let [res1 (sut/timevec->time-str [33 42])
          res2 (sut/timevec->time-str [3 94])]
      (t/is (= res1 "33:42"))
      (t/is (= res2 "4:34")))))

(t/deftest timestr->minutes
  (t/testing "It returns expected output"
    (let [res1 (sut/timestr->minutes "34:42")
          res2 (sut/timestr->minutes "02:40")]
      (t/is (= res1 2082))
      (t/is (= res2 160)))))

(t/deftest timestr->hour-float
  (t/testing "It returns expected output"
    (let [res1 (sut/timestr->hour-float "03:25")
          res2 (sut/timestr->hour-float "02:40")]
      ;; we have to multiply floats by 100 and
      ;; convert to ints to compare them successfully.
      (t/is (=  res1  3.41))
      (t/is (=  res2  2.66)))))

(t/deftest timestr->add-time
  (t/testing "It returns expected output"
    (let [res1 (sut/timestr->add-time "10:02" "00:02")
          res2 (sut/timestr->add-time "2:40" "14:45")]
      (t/is (= res1 "10:04"))
      (t/is (= res2 "17:25")))))

(t/deftest org-date->java-date
  (t/testing "It returns expected output"
    (let [res (sut/org-date->java-date "<2020-05-14 19:11>")
          res2 (sut/org-date->java-date "[2019-12-04 19:11]")]
      (t/is (= java.util.Date (type res)))
      (t/is (= res #inst "2020-05-14T04:00:00.000-00:00"))
      (t/is (= res2 #inst "2019-12-04T00:00:00.000-00:00"))
      (t/is (= java.util.Date (type res2))))))

(t/deftest org-date->ts
  (t/testing "It returns expected output"
    (let [res (sut/org-date->ts "<2020-05-14 19:11>")
          res2 (sut/org-date->ts "[2019-12-04 19:11]")]
      (t/is (= Integer (type res)))
      (t/is (= res 1589428800))
      (t/is (= res2 1575435600)))))

(t/deftest build-year
  (let [year-2020    (sut/build-year 2020)
        year-2019    (sut/build-year 2019)
        sample-day   (dissoc (first year-2019) :date)
        expected-day {:log-sum "00:00", :date-str "2019-01-01" :log-count 0, :logs-raw [], :hour-sum 0}]

    (t/testing "It handles leap years and regular years."
      (t/is (= 366 (count year-2020))) ;; leap year.
      (t/is (= 365 (count year-2019))))
    (t/testing "The day has expected keys"
      (t/is (= sample-day expected-day)))))
