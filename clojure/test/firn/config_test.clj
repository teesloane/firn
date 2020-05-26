(ns firn.config-test
  (:require [firn.config :as sut]
            [clojure.test :as t]))

(t/deftest clean-config
  (t/testing "Permanent keys are not written over."
    (let [valid-ucfg   {:dir-data "data" :ignored-dirs ["priv"]}
          invalid-ucfg {:dir-data "data"
                        :ignored-dirs ["priv"]
                        :dir-files     "foo"
                        :dir-layouts   "foo"
                        :dir-partials  "foo"
                        :dir-site      "foo"
                        :dirname-files "foo"
                        :layouts       "foo"
                        :partials      "foo"
                        :org-files     ["foo"]}]
      (t/is (= valid-ucfg (sut/clean-config valid-ucfg)))
      (t/is (= valid-ucfg (sut/clean-config invalid-ucfg))))))
