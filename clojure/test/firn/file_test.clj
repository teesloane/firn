(ns firn.file-test
  "These tests heavily rely on the demo files that we process in stubs.clj.
  Generally, for each function we should have an org file with the minimum
  required contents for testing."
  (:require [firn.file :as sut]
            [firn.stubs :as stub]
            [clojure.test :as t]
            [sci.core :as sci]
            [firn.util :as u]))


;; This represents the file "object" - a map of value that accumulate from
;; parsing and munging an org file.


(t/use-fixtures :each stub/test-wrapper)

(def sample-file
  {:path     "/Users/tees/Projects/firn/firn/clojure/test/firn/demo_org/file-small.org",
   :as-json  "{\"type\":\"document\",\"pre_blank\":0,\"children\":[{\"type\":\"section\",\"children\":[{\"type\":\"keyword\",\"key\":\"TITLE\",\"value\":\"Firn\",\"post_blank\":0},{\"type\":\"keyword\",\"key\":\"DATE_CREATED\",\"value\":\"<2020-03-01 09:53>\",\"post_blank\":0},{\"type\":\"keyword\",\"key\":\"DATE_UPDATED\",\"value\":\"<2020-04-26 15:43>\",\"post_blank\":0},{\"type\":\"keyword\",\"key\":\"FIRN_UNDER\",\"value\":\"project\",\"post_blank\":0},{\"type\":\"keyword\",\"key\":\"FIRN_LAYOUT\",\"value\":\"project\",\"post_blank\":0}]},{\"type\":\"headline\",\"level\":1,\"children\":[{\"type\":\"title\",\"level\":1,\"raw\":\"Foo\",\"post_blank\":0,\"children\":[{\"type\":\"text\",\"value\":\"Foo\"}]},{\"type\":\"section\",\"children\":[{\"type\":\"paragraph\",\"post_blank\":0,\"children\":[{\"type\":\"text\",\"value\":\"Hi there!\"}]}]}]}]}\n",
   :logbook  (),
   :as-html  "<html>Stub text.</html>",
   :name     "file-small",
   :original nil,
   :path-web "file-small",
   :keywords [{:type "keyword", :key "TITLE", :value "Firn", :post_blank 0} {:type "keyword", :key "DATE_CREATED", :value "<2020-03-01 09:53>", :post_blank 0} {:type "keyword", :key "DATE_UPDATED", :value "<2020-04-26 15:43>", :post_blank 0} {:type "keyword", :key "FIRN_UNDER", :value "project", :post_blank 0} {:type "keyword", :key "FIRN_LAYOUT", :value "project", :post_blank 0}],
   :meta     {}
   :as-edn   {:type "document", :pre_blank 0, :children [{:type "section", :children [{:type "keyword", :key "TITLE", :value "Firn", :post_blank 0} {:type "keyword", :key "DATE_CREATED", :value "<2020-03-01 09:53>", :post_blank 0} {:type "keyword", :key "DATE_UPDATED", :value "<2020-04-26 15:43>", :post_blank 0} {:type "keyword", :key "FIRN_UNDER", :value "project", :post_blank 0} {:type "keyword", :key "FIRN_LAYOUT", :value "default", :post_blank 0}]} {:type "headline", :level 1, :children [{:type "title", :level 1, :raw "Foo", :post_blank 0, :children [{:type "text", :value "Foo"}]} {:type "section", :children [{:type "paragraph", :post_blank 0, :children [{:type "text", :value "Hi there!"}]}]}]}]},
   :links    ()})

(t/deftest get-io-name
  (t/testing "Get a file name extension from a java io object"
    (t/is (= "file1" (sut/get-io-name (stub/gtf :tf-1 :io))))))

(t/deftest get-web-path
  (t/testing "It properly builds webpath"
    (t/is
     (= "baz/foo/test"
        (sut/get-web-path "my-files" "foo/bar/my-files/baz/foo/test.org")))))

(t/deftest make
  (t/testing "Has correct values with the dummy io-file"
    (let [test-file (stub/gtf :tf-1 :io)
          new-file (sut/make (stub/sample-config) test-file)]

      (t/is (= (new-file :name)    "file1"))
      (t/is (= (new-file :path)    (.getPath ^java.io.File test-file)))
      (t/is (= (new-file :path-web) "file1")))))

(t/deftest keywords->map
  (t/testing "A list of keywords gets converted into a map. "
    (let [file-1 (stub/gtf :tf-1 :processed)
          res    (sut/keywords->map file-1)]
      (t/is (= res {:title "Sample File!" :firn-layout "default" :firn-toc {:headline "Notes", :depth 5}})))))

(t/deftest is-private?
  (t/testing "Returns true when a file has a private keywords"
    (let [config             (stub/sample-config)
          file-priv-1        (stub/gtf :tf-private :processed)
          file-priv-2        (stub/gtf :tf-private-subfolder :processed)
          ;; file               stub/test-file-private-processed
          ;; file-2             stub/test-file-private-subfolder-processed
          is-priv?           (sut/is-private? config file-priv-1)
          is-priv-subfolder? (sut/is-private? config file-priv-2)]
      (t/is (= is-priv? true))
      (t/is (= is-priv-subfolder? true)))))

(t/deftest extract-metadata
  (let [file        (stub/gtf :tf-metadata :processed)
        start-times (map #(% :start-ts) (-> file :meta :logbook))]
    (t/testing "Pulls links and logbook entries from a file"
      (t/is (seq (get-in file [:meta :links])))
      (t/is (seq (get-in file [:meta :logbook])))
      (t/is (seq (get-in file [:meta :tags])))
      (t/is (= "File Logbook" (get-in file [:meta :title]))))

    (t/testing "The logbook is associated with a heading."
      (let [first-entries (-> file :meta :logbook first)]
        (t/is (= "A headline with a normal log-book." (first-entries :from-headline)))))

    (t/testing "The tag is associated with a heading."
      (let [first-entries (-> file :meta :tags first)]
        (t/is (= "A headline with a normal log-book." (first-entries :from-headline)))
        (t/is (= "/file-metadata#a-headline-with-a-normal-log-book" (first-entries :headline-link)))
        (t/is (= "File Logbook" (first-entries :from-file)))
        (t/is (= "tag1" (first-entries :tag-value)))))

    (t/testing "check that logbook gets sorted: most-recent -> least-recent by :start-ts"
      ;; a clever way (I borrowed) to check if vals in a list are sorted.
      (t/is (apply >= start-times)))))

(t/deftest sum-logbook
  (t/testing "It returns the expected output"
    (let [file (stub/gtf :tf-metadata :processed)
          res  (sut/sum-logbook (-> file :meta :logbook))]
      (t/is (= res "4:44"))
      (t/is (= (type res) java.lang.String)))))

(t/deftest get-keywords
  (t/testing "A file with keywords returns a vector where each item is a map with a key of :type 'keyword'"
    (let [file-1 (stub/gtf :tf-1 :processed)
          res    (sut/get-keywords file-1)]
      (doseq [keywrd res]
        (t/is (= "keyword" (:type keywrd)))))))

(t/deftest get-keyword
  (t/testing "It returns a keyword"
    (let [file-1 (stub/gtf :tf-1 :processed)
          res    (sut/get-keyword file-1 "FIRN_LAYOUT")]
      (t/is (= "default" res)))))

;; TODO
;; (t/deftest make-site-map-item)

(t/deftest make-site-map
  (let [files [{:title      "Home"
                :firn-order 1000
                :firn-under nil
                :path       "http://localhost:4000/index"}
               {:title      "Configuration"
                :firn-order 1
                :firn-under nil
                :path       "http://localhost:4000/configuration"}
               {:title      "Sample Page"
                :firn-order 7
                :firn-under nil
                :path       "http://localhost:4000/sample-page"}
               {:title      "Data and Metadata"
                :firn-order 5
                :firn-under nil
                :path       "http://localhost:4000/data-and-metadata"}
               {:title      "Firn Setup (with Emacs)"
                :firn-order 4
                :firn-under nil
                :path       "http://localhost:4000/setup"}
               {:title      "Layout"
                :firn-order 2
                :firn-under nil
                :path       "http://localhost:4000/layout"}
               {:title      "Custom Pages"
                :firn-order 3
                :firn-under nil
                :path       "http://localhost:4000/pages"}
               {:firn-order    6
                :firn-under    "Foobar"
                :logbook-total "0:00",
                :path          "http://localhost:4000/limitations"
                :title         "Limitations"}
               {:firn-order 8
                :firn-under ["Contributors" "Things"]
                :path       "http://localhost:4000/changelog"
                :title      "Changelog"}
               {:firn-order 0
                :firn-under ["Contributors" "Things"]
                :path       "http://localhost:4000/getting-started"
                :title      "Getting Started"}
               {:firn-order 8
                :firn-under nil
                :path       "http://localhost:4000/contributors"
                :title      "Contributors"}
               {:firn-order 9999
                :firn-under ["Page"]
                :path       "http://localhost:4000/tags"
                :title      "Tags"}]]
    (t/testing "Files without firn-under are put as top level keys"
      (let [res           (sut/make-site-map files)
            top-keys      (keys res)
            expected-keys '("Foobar" "Firn Setup (with Emacs)" "Data and Metadata" "Custom Pages" "Page" "Sample Page" "Configuration" "Layout" "Home" "Contributors")]

        ;; check that top level keys are present.
        (doseq [k expected-keys] (t/is (u/in? top-keys k)))
        ;; firn-under values should not be top level
        (t/is (not (u/in? top-keys "Changelog")))
        (t/is (not (u/in? top-keys "Getting Started")))
        (t/is (not (u/in? top-keys "Tags")))
        ;; Check that single string firn under went under
        (t/is (= (get-in res ["Foobar" :children])
                 {"Limitations" {:firn-order    6,
                                 :firn-under    "Foobar",
                                 :logbook-total "0:00",
                                 :path          "http://localhost:4000/limitations",
                                 :title         "Limitations"}}))
        (t/is (= (get-in res ["Contributors" :children])
                 {"Things" {:children {"Changelog"       {:firn-order 8, :firn-under ["Contributors" "Things"], :path "http://localhost:4000/changelog", :title  "Changelog"},
                                       "Getting Started" {:firn-order 0, :firn-under ["Contributors" "Things"], :path "http://localhost:4000/getting-started", :title "Getting Started"}}}}))))))
