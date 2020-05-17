(ns firn.file-test
  "These tests heavily rely on the demo files that we process in stubs.clj.
  Generally, for each function we should have an org file with the minimum
  required contents for testing."
  (:require [firn.file :as sut]
            [firn.stubs :as stub]
            [clojure.test :as t]))


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

(t/deftest keywords->map
  (t/testing "A list of keywords gets converted into a map. "
    (let [file-1 (stub/gtf :tf-1 :processed)
          res    (sut/keywords->map file-1)]
      (t/is (= res {:title "Sample File!" :firn-layout "default"}))
      res)))

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
      (t/is (= "File Logbook" (get-in file [:meta :title]))))

    (t/testing "The logbook is associated with a heading."
      (let [first-entries (-> file :meta :logbook first)]
        (t/is (= "A headline with a normal log-book." (first-entries :from-headline)))))

    (t/testing "check that logbook gets sorted: most-recent -> least-recent by :start-ts"
      ;; a clever way (I borrowed) to check if vals in a list are sorted.
      (t/is (apply >= start-times)))))

(t/deftest htmlify
  (let [sample-file   (dissoc sample-file :as-html)
        sample-config (stub/sample-config)
        htmlified     (sut/htmlify sample-config sample-file)]
    (t/testing "has :as-html config"
      (t/is (contains? htmlified :as-html)))))

(t/deftest process-one
  (let [test-file     (stub/gtf :tf-1 :io)
        sample-config (stub/sample-config)
        processed     (sut/process-one sample-config test-file)]
    (t/is (every? #(contains? processed %)
                  [:path :as-json  :meta     :as-html
                   :name :original :path-web :keywords  :as-edn]))))

(t/deftest sum-logbook
  (t/testing "It returns the expected output"
    (let [file (stub/gtf :tf-metadata :processed)
          res  (sut/sum-logbook (-> file :meta :logbook))]
      (t/is (= res "4:44"))
      (t/is (= (type res) java.lang.String)))))

;; (t/deftest footnote-file
;;   (t/testing "It parses footnotes"
;;     (let [config             (stub/sample-config)
;;           file-footnotes     (stub/gtf :tf-footnotes :processed)]
;;       (prn file-footnotes)
;;       (t/is (= true true)))))
