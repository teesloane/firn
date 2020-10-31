(ns firn.build-test
  (:require [firn.build :as sut]
            [firn.stubs :as stub]
            [firn.util :as u]
            [me.raynes.fs :as fs]
            [clojure.test :as t]))

(t/use-fixtures :each stub/test-wrapper)

(t/deftest new-site
  ;; The _firn site shouldn't exist yet when new-site is called.
  (t/testing "The _firn site shouldn't exist yet"
    (t/is (= false (fs/exists? stub/firn-dir))))

  (sut/new-site {:dir stub/test-dir})

  (t/testing "Creates a new site with the proper structure."
    (doseq [file-path sut/default-files
            :let [file-path-full     (str stub/firn-dir "/" file-path)
                  file-string-length (-> file-path-full slurp count)]]
      (t/is (= true (fs/exists? file-path-full)))
      ;; each file should have a string of contents > 0
      (t/is (> file-string-length 0))))

  ;; NOTE: I'm not sure how to test System/exit yet -
  #_(t/testing "Trying to create again when _firn already exists should return false"
      (t/is (= ????? (sut/new-site {:dir-files stub/test-dir})))))

(t/deftest setup
  ;; setup requires that you have the _firn site in place; so:
  (sut/new-site {:dir stub/test-dir})

  (let [config      (stub/sample-config)
        setup-config (sut/setup config)]
    (t/testing "expect org-files, layouts and partials to be a key in config"
      (t/is (= true (empty? (config :org-files))))
      (t/is (= true (empty? (config :layouts))))
      (t/is (= true (empty? (config :partials))))

      (t/is (= true (not (empty? (setup-config :org-files)))))
      (t/is (= true (not (empty? (setup-config :layouts)))))
      (t/is (= true (not (empty? (setup-config :partials))))))

    (t/testing "_site should be created"
      (t/is (= true (fs/exists? (setup-config :dir-site))))))

  (stub/delete-firn-dir))


;; Something weird happening here. Revisit later.
#_(t/deftest htmlify
    (let [sample-file   (dissoc file-test/sample-file :as-html)
          sample-config (stub/sample-config)
          htmlified     (sut/htmlify sample-config sample-file)]
      (t/testing "has :as-html config"
        (t/is (contains? htmlified :as-html)))))

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
                :firn-under    ["Foobar"]
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
                                 :firn-under    ["Foobar"],
                                 :logbook-total "0:00",
                                 :path          "http://localhost:4000/limitations",
                                 :title         "Limitations"}}))
        (t/is (= (get-in res ["Contributors" :children])
                 {"Things" {:children {"Changelog"       {:firn-order 8, :firn-under ["Contributors" "Things"], :path "http://localhost:4000/changelog", :title  "Changelog"},
                                       "Getting Started" {:firn-order 0, :firn-under ["Contributors" "Things"], :path "http://localhost:4000/getting-started", :title "Getting Started"}}}}))))))
