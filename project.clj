(defproject firn "0.1.0-SNAPSHOT"
  :description "Wiki Generator"
  :plugins [[lein-cljfmt "0.6.7"]]
  :url "http://example.com/FIXME"
  :dependencies [[cheshire "5.10.0"]
                 [hiccup "1.0.5"]
                 [me.raynes/fs "1.4.6"]
                 [org.clojure/clojure "1.10.0"]]
  :main ^:skip-aot firn.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
