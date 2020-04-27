(ns firn.file-test
  (:require [firn.file :as sut]
            [firn.stubs :as stub]
            [clojure.test :as t]))


;; This represents the file "object" - a map of value that accumulate from
;; parsing and munging an org file.

(t/use-fixtures :each stub/test-wrapper)

(def sample-file
  {:path      "/Users/tees/Projects/firn/firn/clojure/test/firn/demo_org/file-small.org",
   :as-json   "{\"type\":\"document\",\"pre_blank\":0,\"children\":[{\"type\":\"section\",\"children\":[{\"type\":\"keyword\",\"key\":\"TITLE\",\"value\":\"Firn\",\"post_blank\":0},{\"type\":\"keyword\",\"key\":\"DATE_CREATED\",\"value\":\"<2020-03-01 09:53>\",\"post_blank\":0},{\"type\":\"keyword\",\"key\":\"DATE_UPDATED\",\"value\":\"<2020-04-26 15:43>\",\"post_blank\":0},{\"type\":\"keyword\",\"key\":\"FIRN_UNDER\",\"value\":\"project\",\"post_blank\":0},{\"type\":\"keyword\",\"key\":\"FIRN_LAYOUT\",\"value\":\"project\",\"post_blank\":0}]},{\"type\":\"headline\",\"level\":1,\"children\":[{\"type\":\"title\",\"level\":1,\"raw\":\"Foo\",\"post_blank\":0,\"children\":[{\"type\":\"text\",\"value\":\"Foo\"}]},{\"type\":\"section\",\"children\":[{\"type\":\"paragraph\",\"post_blank\":0,\"children\":[{\"type\":\"text\",\"value\":\"Hi there!\"}]}]}]}]}\n",
   :logbook   (),
   :as-html   "<html>Stub text. Not testing this for now.</html>",
   :name      "file-small",
   :original  nil,
   :path-web  "file-small",
   :keywords  [{:type "keyword", :key "TITLE", :value "Firn", :post_blank 0} {:type "keyword", :key "DATE_CREATED", :value "<2020-03-01 09:53>", :post_blank 0} {:type "keyword", :key "DATE_UPDATED", :value "<2020-04-26 15:43>", :post_blank 0} {:type "keyword", :key "FIRN_UNDER", :value "project", :post_blank 0} {:type "keyword", :key "FIRN_LAYOUT", :value "project", :post_blank 0}],
   :org-title "Firn",
   :as-edn {:type      "document", :pre_blank 0, :children [{:type "section", :children [{:type "keyword", :key "TITLE", :value "Firn", :post_blank 0} {:type "keyword", :key "DATE_CREATED", :value "<2020-03-01 09:53>", :post_blank 0} {:type "keyword", :key "DATE_UPDATED", :value "<2020-04-26 15:43>", :post_blank 0} {:type "keyword", :key "FIRN_UNDER", :value "project", :post_blank 0} {:type "keyword", :key "FIRN_LAYOUT", :value "project", :post_blank 0}]} {:type  "headline", :level 1, :children [{:type       "title", :level      1, :raw        "Foo", :post_blank 0, :children   [{:type "text", :value "Foo"}]} {:type "section", :children [{:type       "paragraph", :post_blank 0, :children   [{:type "text", :value "Hi there!"}]}]}]}]},
   :links     ()})


(t/deftest strip-file-ext
  (t/testing "it properly strips extensions."
    (t/is (= "foo" (sut/strip-file-ext "org" "foo.org")))
    (t/is (= "my-file" (sut/strip-file-ext "jpeg" "my-file.jpeg")))))

(t/deftest get-io-name
  (t/testing "Get a file name extension from a java io object"
    (t/is (= "file1" (sut/get-io-name stub/test-file-1)))))

(t/deftest get-web-path
  (t/testing "It properly builds webpath"
    (t/is
     (= "baz/foo/test"
        (sut/get-web-path "my-files" "foo/bar/my-files/baz/foo/test.org"))))
  (t/testing "It returns false (and print an error msg when an invalid path.)"
    (t/is
     (= false
        (sut/get-web-path "my-files" "foo/bar/my-files/baz/my-files/test.org")))))

(t/deftest make
  (t/testing "Has correct values with the dummy io-file"
    (let [new-file (sut/make (stub/sample-config)
                             stub/test-file-1)]

      (t/is (= (new-file :name)    "file1"))
      (t/is (= (new-file :path)    (.getPath ^java.io.File stub/test-file-1)))
      (t/is (= (new-file :path-web) "file1")))))

(t/deftest get-keywords
  (t/testing "A file with keywords returns a vector where each item is a map with a key of :type 'keyword'"
    (let [res (sut/get-keywords stub/test-file-1-processed)]
      (doseq [keywrd res]
        (t/is (= "keyword" (:type keywrd))))))

  #_(t/testing "A file without keywords should return false"
      (let [res (sut/get-keywords stub/test-file-no-keywords-processed)]
        (t/is (= false res))))) ;; FIXME - should throw an error?

(t/deftest get-keyword
  (t/testing "It returns a keyword"
    (let [res (sut/get-keyword stub/test-file-1-processed "FIRN_LAYOUT")]
      (t/is (= "default" res)))))

(t/deftest keywords->map
  (t/testing "A list of keywords gets converted into a map. "
    (let [res (sut/keywords->map stub/test-file-1-processed)]
      (t/is (= res {:title "Sample File!" :firn-layout "default"}))
      res)))

(t/deftest is-private?
  (t/testing "Returns true when a file has a private keywords"
    (let [config             (stub/sample-config)
          file               stub/test-file-private-processed
          file-2             stub/test-file-private-subfolder-processed
          is-priv?           (sut/is-private? config file)
          is-priv-subfolder? (sut/is-private? config file-2)]
      (t/is(= is-priv? true))
      (t/is(= is-priv-subfolder? true)))))
