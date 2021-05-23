(ns firn.org-test
  (:require [firn.org :as sut]
            [clojure.test :as t]
            [firn.stubs :as stub]
            [firn.org :as org]))

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

(t/deftest get-link-parts
  (t/testing "Expected output"
    (let [res1 (sut/get-link-parts "file:foo_bar.org")
          res2 (sut/get-link-parts "file:foo_bar.org::*a heading")]
      (t/is (={:anchor nil, :slug "foo_bar"} res1))
      (t/is (={:anchor "#a-heading", :slug "foo_bar"} res2)))))

(t/deftest parsed-org-date->unix-time
  (t/testing "returns the expected value."
    (t/is (= 1585683360000
             (sut/parsed-org-date->unix-time (sample-logentry :start) (stub/gtf :tf-1 :processed))))))

(t/deftest make-headline-anchor
  (t/testing "expected output"
    (let [sample-headline
          {:type "headline",
           :level 1,
           :children [{:type "title", :level 1, :raw "My Headline.", :children [{:type "text", :value "My Headline."}]}]}
          res (sut/make-headline-anchor sample-headline)]
      (t/is (= res "#my-headline")))))

(t/deftest internal-link-handler
  (t/testing "Expected results."
    (let [res1                 (sut/internal-link-handler  {:org-link "file:foo.org" :site-url "http://mysite.com" })
          res2                 (sut/internal-link-handler  {:org-link "file:foo.org::*my headline link" :site-url "http://mysite.com"})
          res-from-nested-file (sut/internal-link-handler  {:org-link "file:foo.org::*my headline link" :site-url "http://mysite.com" :file {:path-web "bar/test"}})
          res-up-dir           (sut/internal-link-handler  {:org-link "file:../foo.org" :site-url "http://mysite.com" :file {:path-web "lvl1/lvl2/lvl3"}})]
      (t/is (= res1 "http://mysite.com/foo"))
      (t/is (= res2 "http://mysite.com/foo#my-headline-link"))
      (t/is (= res-up-dir "http://mysite.com/lvl1/foo"))
      (t/is (= res-from-nested-file "http://mysite.com/bar/foo#my-headline-link")))))

;;;;;;;;;; MIGRATED TESTS -------------------------------------

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

(t/deftest make
  (t/testing "Has correct values with the dummy io-file"
    (let [test-file (stub/gtf :tf-1 :io)
          new-file (sut/make-file (stub/sample-config) test-file)]

      (t/is (= (new-file :name)    "file1"))
      (t/is (= (new-file :path)    (.getPath ^java.io.File test-file)))
      (t/is (= (new-file :path-web) "file1")))))

(t/deftest parse-front-matter->map
  (t/testing "A list of keywords gets converted into a map. "
    (let [file-1 (stub/gtf :tf-1 :processed)
          res    (sut/parse-front-matter->map file-1)]
      (t/is (= res
               {:date-created "<2020-08-17 Mon>",
                :date-updated "<2020-08-17 Mon>",
                :firn-layout "default",
                :firn-order 1,
                :firn-toc {:depth 5, :headline "Notes"},
                :firn-under "Research",
                :roam-tags "foo bar"
                :title "Org Mode"}
               )))))

(t/deftest is-private?
  (t/testing "Returns true when a file has a private keywords"
    (let [config             (stub/sample-config)
          file-priv-1        (stub/gtf :tf-private :processed)
          file-priv-2        (stub/gtf :tf-private-subfolder :processed)
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

    (t/testing "The TODO keyword is recognized in a headline"
      (let [first-entries (-> file :meta :todos)]
        (t/is (= first-entries [{:from-headline "A todo thing.",
                                 :headline-link "/file-metadata#a-todo-thing",
                                 :keyword "TODO"}]))))

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

;; TODO: flakey test on CI.
;; (t/deftest make-site-map-item
;;   (t/testing "Proper data is discarded from a file"
;;     (let [pf             (stub/gtf :tf-1 :processed)
;;           res            (sut/make-site-map-item pf "http://my-site-url.com")
;;           forbidden-keys '(:logbook :links :toc :keywords :tags :attachments) ]
;;       (t/is (= res {:date-created    "2020-08-17 Mon",
;;                     :date-created-ts 1597636800,
;;                     :date-updated    "2020-08-17 Mon",
;;                     :date-updated-ts 1597636800,
;;                     :firn-order      1,
;;                     :firn-under      ["Research"],
;;                     :logbook-total   "12:27",
;;                     :firn-tags       nil
;;                     :path            "http://my-site-url.com/file1",
;;                     :title           "Org Mode"}))
;;       (doseq [k forbidden-keys]
;;         (t/is (not (u/in? res k)))))))

(t/deftest make-file-tags
  (t/testing "expected output"
    (let [test-input {:file-tags       "foo bar"
                      :date-created-ts 1592625600
                      :file-metadata   {:from-file "Configuration", :from-url "http://localhost:4000/configuration"}}
          expected   '({:date-created-ts 1592625600,
                        :from-file       "Configuration",
                        :from-url        "http://localhost:4000/configuration",
                        :tag-value       "foo"}
                       {:date-created-ts 1592625600,
                        :from-file       "Configuration",
                        :from-url        "http://localhost:4000/configuration",
                        :tag-value       "bar"})
          res        (sut/make-file-tags test-input)]
      (t/is (= res expected)))))
