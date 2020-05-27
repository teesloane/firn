(ns firn.markup-test
  (:require [firn.markup :as sut]
            [clojure.test :as t]))


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

;; Tests

(t/deftest link->html
  (t/testing "http-link"
    (t/is (= (sut/link->html (sample-links :http-link))
             [:a.firn_external
              {:href "https://docs.cider.mx/cider/usage/misc_features.html" :target "_blank"}
              "Miscellaneous Features :: CIDER Docs"])))

  (t/testing "http-image-link"
    (t/is (= (sut/link->html (sample-links :http-img))
             [:span.firn-img-with-caption
              [:img {:src "https://www.fillmurray.com/g/200/300.jpg"}]
              [:span.firn-img-caption "Fill murray"]])))
           

  (t/testing "img-link"
    (t/is (= (sut/link->html (sample-links :img-file))
             [:img {:src "test-img.png"}])))

  (t/testing "img-rel-file"
    (t/is (= (sut/link->html (sample-links :img-rel-file))
             [:img {:src "./static/images/test-img.png"}])))

  (t/testing "internal-link"
    (t/is (= (sut/link->html (sample-links :file-link))
             [:a.firn_internal {:href "./file2"} "File 2"]))))

(t/deftest internal-link-handler
  (t/testing "Expected results."
    (let [res1 (sut/internal-link-handler "file:foo.org")
          res2 (sut/internal-link-handler "file:foo.org::*my headline link")]
      (t/is (= res1 "./foo"))
      (t/is (= res2 "./foo#my-headline-link")))))
