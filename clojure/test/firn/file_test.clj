(ns firn.file-test
  (:require [firn.file :as sut]
            [firn.stubs :as stubs]
            [clojure.test :as t]))


;; This represents the file "object" - a map of value that accumulate from
;; parsing and munging an org file.


(def dummy-file
  {}) ;; TODO

(t/deftest strip-file-ext
  (t/testing "it properly strips extensions."
    (t/is (= "foo" (sut/strip-file-ext "org" "foo.org")))
    (t/is (= "my-file" (sut/strip-file-ext "jpeg" "my-file.jpeg")))))

(t/deftest get-io-name
  (t/testing "Get a file name extension from a java io object"
    (t/is (= "file1" (sut/get-io-name stubs/test-file-1)))))

(t/deftest get-web-path
  (t/testing "It properly builds webpath"
    (t/is
     (= "baz/foo/test"
        (sut/get-web-path "my-files" "foo/bar/my-files/baz/foo/test.org"))))
  (t/testing "It returns false (and print an error msg when an invalid path.)"
    (t/is
     (= false
        (sut/get-web-path "my-files" "foo/bar/my-files/baz/my-files/test.org")))))
