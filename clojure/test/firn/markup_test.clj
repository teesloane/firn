(ns firn.markup-test
  (:require [firn.markup :as sut]
            [clojure.test :as t]
            [firn.stubs :as stub]
            [firn.build :as build]))

;; Mocks

(def sample-links {:img-file     {:type "link", :path "file:test-img.png"}
                   :img-rel-file {:type "link", :path "./static/images/test-img.png"}
                   :file-link    {:type "link", :path "file:file2.org", :desc "File 2"}
                   :http-img     {:type "link",
                                  :path "https://www.fillmurray.com/g/200/300.jpg",
                                  :desc "Fill murray"}
                   :http-link    {:type "link",
                                  :path "https://docs.cider.mx/cider/usage/misc_features.html",
                                  :desc "Miscellaneous Features :: CIDER Docs"}})

(def sample-sitemap
  {"Writing"  {:path "http://localhost:4000/writing", :date-created "2020-05-04 15:10", :date-updated "2020-08-11 15:56", :firn-under nil, :date-created-ts 1588564800, :title "Writing", :firn-order 0, :date-updated-ts 1588564800, :logbook-total "0:00"},
   "Research" {:path     "http://localhost:4000/research",
               :children {"Quil"            {:path "http://localhost:4000/quil", :date-created "2020-02-28 08:31", :date-updated "2020-08-04 20:59", :firn-under ["Research"], :date-created-ts 1582866000, :title "Quil",  :date-updated-ts 1582866000, :logbook-total "0:00"},
                          "Generative Art"  {:path "http://localhost:4000/generative_art", :date-created "2020-06-02 Tue", :date-updated "2020-08-04 20:51", :firn-under ["Research"], :date-created-ts 1591070400, :title "Generative Art", :firn-order 2, :date-updated-ts 1591070400, :logbook-total "0:00"},
                          "Org Mode"        {:path "http://localhost:4000/org-mode", :date-created "2020-02-28 20:56", :date-updated "2020-08-04 21:02", :firn-under ["Research"], :date-created-ts 1582866000, :title "Org Mode", :firn-order 3, :date-updated-ts 1582866000, :logbook-total "0:00"},
                          "Open Frameworks" {:path "http://localhost:4000/open_frameworks", :date-created "2020-07-08 15:48", :date-updated "2020-08-06 17:35", :firn-under ["Research"], :date-created-ts 1594180800, :title "Open Frameworks", :firn-order 4, :date-updated-ts 1594180800, :logbook-total "0:00"}}}})

;; Tests --

(t/deftest link->html
  (t/testing "http-link"
    (t/is (= (sut/link->html (sample-links :http-link) {:site-url ""})
             [:a.firn-external
              {:href "https://docs.cider.mx/cider/usage/misc_features.html" :target "_blank"}
              "Miscellaneous Features :: CIDER Docs"])))

  (t/testing "http-image-link"
    (t/is (= (sut/link->html (sample-links :http-img) {:site-url ""})
             [:span.firn-img-with-caption
              [:img {:src "https://www.fillmurray.com/g/200/300.jpg"}]
              [:span.firn-img-caption "Fill murray"]])))

  (t/testing "img-link"
    (t/is (= (sut/link->html (sample-links :img-file) {:site-url "http://foo.com"})
             [:img {:src "http://foo.com/test-img.png"}])))

  (t/testing "internal-link"
    (t/is (= (sut/link->html (sample-links :file-link) {:site-url ""})
             [:a.firn-internal {:href "/file2"} "File 2"]))))

(t/deftest internal-link-handler
  (t/testing "Expected results."
    (let [res1 (sut/internal-link-handler "file:foo.org" "http://mysite.com")
          res2 (sut/internal-link-handler "file:foo.org::*my headline link" "http://mysite.com")]
      (t/is (= res1 "http://mysite.com/foo"))
      (t/is (= res2 "http://mysite.com/foo#my-headline-link")))))

(t/deftest make-toc
  (let [ex1     [{:level 1, :text "Process" :anchor "#process"}
                 {:level 2, :text "Relevance" :anchor "#relevance"}]
        ex1-res [:ol [:li
                      [:a {:href "#process"} "Process"]
                      [:ol [[:li [:a {:href "#relevance"} "Relevance"]]]]]]

        ex2     [{:level 1, :text "Process" :anchor "#process"}
                 {:level 2, :text "Relevance" :anchor "#relevance"}
                 {:level 3, :text "Level3" :anchor "#level3"}
                 {:level 2, :text "Level2-again" :anchor "#level2-again"}
                 {:level 4, :text "Foo" :anchor "#foo"}]

        ex2-res [:ol
                 [:li
                  [:a {:href "#process"} "Process"]
                  [:ol
                   '([:li
                      [:a {:href "#relevance"} "Relevance"]
                      [:ol ([:li [:a {:href "#level3"} "Level3"]])]]
                     [:li
                      [:a {:href "#level2-again"} "Level2-again"]
                      [:ol ([:li [:a {:href "#foo"} "Foo"]])]])]]]]

    (t/testing "expected results, no options."
      (let [res  (sut/make-toc ex1)
            res2 (sut/make-toc ex2)]
        (t/is (= res ex1-res))
        (t/is (= res2 ex2-res))))
    (t/testing "Depth limits work."
      (let [res-d1      (sut/make-toc ex2 {:depth 1})
            res-d2      (sut/make-toc ex2 {:depth 2})
            expected-d1 [:ol [:li [:a {:href "#process"} "Process"]]]
            expected-d2 [:ol
                         [:li [:a {:href "#process"} "Process"]
                          [:ol
                           '([:li [:a {:href "#relevance"} "Relevance"]]
                             [:li [:a {:href "#level2-again"} "Level2-again"]])]]]]
        (t/is (= res-d1 expected-d1))
        (t/is (= res-d2 expected-d2))))

    (t/testing "headline select works"
      (let [res-1 (sut/make-toc ex2 {:headline "Relevance"})
            expected [:ol
                      [:li
                       [:a {:href "#relevance"} "Relevance"]
                       [:ol '([:li [:a {:href "#level3"} "Level3"]])]]]]
        (t/is (= res-1 expected))))))

(t/deftest render-sitemap
  (t/testing "With no options, an html site map is returned in the order the site-map as data exists"
    (let [res          (sut/render-site-map sample-sitemap)
          expected-out [:ul.firn-sitemap.firn-sitemap-item--parent
                        '([:li [:a.firn-sitemap-item--link {:href "http://localhost:4000/writing"} "Writing"]]
                          [:li.firn-sitemap-item--child [:a.firn-sitemap-item--link {:href "http://localhost:4000/research"} "Research"]
                           [:ul.firn-sitemap-item--parent
                            ([:li [:a.firn-sitemap-item--link {:href "http://localhost:4000/quil"} "Quil"]]
                             [:li [:a.firn-sitemap-item--link {:href "http://localhost:4000/generative_art"} "Generative Art"]]
                             [:li [:a.firn-sitemap-item--link {:href "http://localhost:4000/org-mode"} "Org Mode"]]
                             [:li [:a.firn-sitemap-item--link {:href "http://localhost:4000/open_frameworks"} "Open Frameworks"]])]])]]
      (t/is (= res expected-out))))

  (t/testing "Firn-order sort works"
    (let [res          (sut/render-site-map sample-sitemap {:sort-by :firn-order})
          expected-out [:ul.firn-sitemap.firn-sitemap-item--parent
                        '([:li [:a.firn-sitemap-item--link {:href "http://localhost:4000/writing"} "Writing"]]
                          ;; Research comes last at top level because it has nil :firn-order
                          [:li.firn-sitemap-item--child [:a.firn-sitemap-item--link {:href "http://localhost:4000/research"} "Research"]
                           [:ul.firn-sitemap-item--parent
                            ([:li [:a.firn-sitemap-item--link {:href "http://localhost:4000/generative_art"} "Generative Art"]]
                             [:li [:a.firn-sitemap-item--link {:href "http://localhost:4000/org-mode"} "Org Mode"]]
                             [:li [:a.firn-sitemap-item--link {:href "http://localhost:4000/open_frameworks"} "Open Frameworks"]]
                             ;; quil comes last as it has nil :firn-order
                             [:li [:a.firn-sitemap-item--link {:href "http://localhost:4000/quil"} "Quil"]])]])]]

      (t/is (= res expected-out))))
  (t/testing "Alphabetical sorting works"
    (let [res          (sut/render-site-map sample-sitemap {:sort-by :alphabetical})
          expected-out [:ul.firn-sitemap.firn-sitemap-item--parent
                        '([:li.firn-sitemap-item--child [:a.firn-sitemap-item--link {:href "http://localhost:4000/research"} "Research"]
                           [:ul.firn-sitemap-item--parent
                            ([:li [:a.firn-sitemap-item--link {:href "http://localhost:4000/generative_art"} "Generative Art"]]
                             [:li [:a.firn-sitemap-item--link {:href "http://localhost:4000/open_frameworks"} "Open Frameworks"]]
                             [:li [:a.firn-sitemap-item--link {:href "http://localhost:4000/org-mode"} "Org Mode"]]
                             [:li [:a.firn-sitemap-item--link {:href "http://localhost:4000/quil"} "Quil"]])]]
                          [:li [:a.firn-sitemap-item--link {:href "http://localhost:4000/writing"} "Writing"]])]]
      (t/is (= res expected-out))))

  (t/testing "Sorting by oldest works"
    (let [res           (sut/render-site-map sample-sitemap {:sort-by :oldest})
          expected-out  [:ul.firn-sitemap.firn-sitemap-item--parent '([:li [:a.firn-sitemap-item--link {:href "http://localhost:4000/writing"} "Writing"]] [:li.firn-sitemap-item--child [:a.firn-sitemap-item--link {:href "http://localhost:4000/research"} "Research"] [:ul.firn-sitemap-item--parent ([:li [:a.firn-sitemap-item--link {:href "http://localhost:4000/quil"} "Quil"]] [:li [:a.firn-sitemap-item--link {:href "http://localhost:4000/org-mode"} "Org Mode"]] [:li [:a.firn-sitemap-item--link {:href "http://localhost:4000/generative_art"} "Generative Art"]] [:li [:a.firn-sitemap-item--link {:href "http://localhost:4000/open_frameworks"} "Open Frameworks"]])]])]]
      (t/is (= res expected-out))))

  (t/testing "Sorting by newest works"
    (let [res          (sut/render-site-map sample-sitemap {:sort-by :newest})
          expected-out [:ul.firn-sitemap.firn-sitemap-item--parent
                        '([:li
                           [:a.firn-sitemap-item--link {:href "http://localhost:4000/writing"} "Writing"]]
                          [:li.firn-sitemap-item--child [:a.firn-sitemap-item--link {:href "http://localhost:4000/research"} "Research"]
                           [:ul.firn-sitemap-item--parent
                            ([:li [:a.firn-sitemap-item--link {:href "http://localhost:4000/open_frameworks"} "Open Frameworks"]]
                             [:li [:a.firn-sitemap-item--link {:href "http://localhost:4000/generative_art"} "Generative Art"]]
                             [:li [:a.firn-sitemap-item--link {:href "http://localhost:4000/quil"} "Quil"]]
                             [:li [:a.firn-sitemap-item--link {:href "http://localhost:4000/org-mode"} "Org Mode"]])]])]]
      (t/is (= res expected-out))))

  (t/testing ":start-at truncates to a child node."
    (let [res          (sut/render-site-map sample-sitemap {:start-at ["Research"] :sort-by :newest})
          expected-out [:ul.firn-sitemap.firn-sitemap-item--parent
                        '([:li [:a.firn-sitemap-item--link {:href "http://localhost:4000/open_frameworks"} "Open Frameworks"]]
                          [:li [:a.firn-sitemap-item--link {:href "http://localhost:4000/generative_art"} "Generative Art"]]
                          [:li [:a.firn-sitemap-item--link {:href "http://localhost:4000/quil"} "Quil"]]
                          [:li [:a.firn-sitemap-item--link {:href "http://localhost:4000/org-mode"} "Org Mode"]])]]

      (t/is (= res expected-out)))))

(t/deftest render-breadcrumbs
(t/testing "expected output"
  (let [res (sut/render-breadcrumbs ["Research" "Writing"] sample-sitemap {})]
    (t/is (= res [:div.firn-breadcrumbs '([:a {:href "http://localhost:4000/research"} "Research"] [:span " > "] [:span "Writing"])]))))

(t/testing "Separator works"
  (let [res (sut/render-breadcrumbs ["Research" "Writing"] sample-sitemap {:separator " | "})]
    (t/is (= res [:div.firn-breadcrumbs '([:a {:href "http://localhost:4000/research"} "Research"] [:span " | "] [:span "Writing"])]))))

(t/testing "Value not found in sitemap returns a plain text span"
  (let [res (sut/render-breadcrumbs ["Foobar"] sample-sitemap {})]
    (t/is (= res [:div.firn-breadcrumbs '([:span "Foobar"])])))))

(t/deftest render-adjacent-file
;; In these tests the "params" map is mimicking what would have been pulled off the file map in a render call.
(t/testing "Defaults to return prev/next html tags by firn-order"
  (let [params   {:sitemap sample-sitemap :firn-under ["Research"] :firn-order 3}
        res      (sut/render-adjacent-file params)
        expected [:div.firn-file-navigation
                  [:span.firn-file-nav-prev "Previous: " [:a {:href "http://localhost:4000/generative_art"} "Generative Art"]]
                  " "
                  [:span.firn-file-nav-next "Next: " [:a {:href "http://localhost:4000/open_frameworks"} "Open Frameworks"]]]]
    (t/is (= res expected))))


(t/testing "When passed options for order-by date, it returns next and prev by proper date."
  (let [params   {:sitemap         sample-sitemap :firn-under ["Research"]
                  :firn-order      3
                  :date-created-ts 1591070400
                  :order-by        :date}
        res      (sut/render-adjacent-file params)
        expected [:div.firn-file-navigation
                  [:span.firn-file-nav-prev "Previous: " [:a {:href "http://localhost:4000/org-mode"} "Org Mode"]]
                  " "
                  [:span.firn-file-nav-next "Next: " [:a {:href "http://localhost:4000/open_frameworks"} "Open Frameworks"]]]]
    (t/is (= res expected))))


  (t/testing "When passed options for changing prev/next text, it does so."
    (let [params   {:sitemap         sample-sitemap :firn-under ["Research"]
                    :firn-order      3
                    :date-created-ts 1591070400
                    :order-by        :date
                    :prev-text       "<--"
                    :next-text        "-->"}
          res      (sut/render-adjacent-file params)
          expected [:div.firn-file-navigation
                    [:span.firn-file-nav-prev "<--" [:a {:href "http://localhost:4000/org-mode"} "Org Mode"]]
                    " "
                    [:span.firn-file-nav-next "-->" [:a {:href "http://localhost:4000/open_frameworks"} "Open Frameworks"]]]]
      (t/is (= res expected))))

  (t/testing ":as-data? responds with just data."
    (let [params {:sitemap sample-sitemap :firn-under ["Research"] :firn-order 3 :as-data? true}
          res    (sut/render-adjacent-file params)]
      (t/is (= res {:next     (get-in sample-sitemap ["Research" :children "Open Frameworks"])
                    :previous (get-in sample-sitemap ["Research" :children "Generative Art"])})))))

(t/deftest render-backlinks
  (t/testing "Expected results"
    (let [
          test-file     (stub/gtf :tf-small :processed)
          sample-config (-> (stub/sample-config) build/setup build/process-all)
          res           (sut/render-backlinks  {:site-links (sample-config :site-links)
                                                :site-links-private (sample-config :site-links-private)
                                                :site-url   ""
                                                :file       test-file})]
      (t/is (= res [:ul.firn-backlinks [:li.firn-backlink [:a {:href "/file1"} "Org Mode"]]]))
      (t/is (not= res [:ul.firn-backlinks
                       [:li.firn-backlink [:a {:href "/file1"} "Org Mode"]]
                       [:li.firn-backlink [:a {:href "/file-private"} "File Private"]]])))))
