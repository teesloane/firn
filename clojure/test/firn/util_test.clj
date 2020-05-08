(ns firn.util-test
  (:require [firn.util :as sut]
            [firn.stubs :as stub]
            [clojure.test :as t]
            [me.raynes.fs :as fs]))

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
  (let [res (sut/io-file->keyword (stub/gtf :tf-1 :io))]
    (t/testing "a file name becomes a keyword"
      (t/is (= :file1 res)))))

(t/deftest find-files-by-ext
  (t/testing "It finds the demo_org org files."
    (let [files (sut/find-files-by-ext stub/test-dir "org")]
      (t/is (= 7 (count files))) ; not the best test, but only has to be updated when number of org sample files change.
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
