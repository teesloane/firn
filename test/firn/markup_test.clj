(ns firn.markup-test
  (:require [firn.markup :as sut]
            [clojure.test :as t]))


(def sample-links {:file-img   {:type "link", :path "file:test-img.png"}
                   ;; :string-img {:type "link", :path "./test-img.png"} ;; not supported yet.
                   :http-link  {:type "link",
                                :path "https://docs.cider.mx/cider/usage/misc_features.html",
                                :desc "Miscellaneous Features :: CIDER Docs"}})


(t/deftest link->html
  (t/testing "http-link"
    (t/is (= (sut/link->html (sample-links :http-link))
           [:a
            {:href "https://docs.cider.mx/cider/usage/misc_features.html"}
            "Miscellaneous Features :: CIDER Docs"])))

  (t/testing "img-link"
    (t/is (= (sut/link->html (sample-links :file-img))
           [:img {:src "test-img.png"}])))

  (t/testing "internal-link"
    (t/is (= (sut/link->html (sample-links :file-img))
           [:img {:src "test-img.png"}]))))
