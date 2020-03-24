(defproject firn "0.1.0-SNAPSHOT"
  :description "Wiki Generator"
  :plugins [[lein-cljfmt "0.6.7"]
            [io.taylorwood/lein-native-image "0.3.1"]]
  :url "http://example.com/FIXME"
  :dependencies [[cheshire "5.10.0"]
                 [hiccup "1.0.5"]
                 [me.raynes/fs "1.4.6"]
                 ;; [org.clojure/clojure "1.10.0"]
                 [cli-matic "0.3.11"]
                 [org.clojure/clojure "1.9.0"]]

  :resource-paths ["resources"]

  :native-image {:name      "firn" ;; name of output image, optional
                 :graal-bin "/Users/tees/Downloads/graalvm-ce-java11-20.0.0/Contents/Home/bin/native-image" ;; path to GraalVM home, optional
                 :opts     ["--report-unsupported-elements-at-runtime"
                            "--initialize-at-build-time"
                            "--allow-incomplete-classpath"
                            ;;avoid spawning build server
                            ;; "-H:+PrintAnalysisCallTree" ;; < for finding deps / things that are reflected.
                            "--no-server"
                            "-H:EnableURLProtocols=https"]}
                            ;; "-H:ReflectionConfigurationFiles=reflect-config.json"

  :main ^:skip-aot firn.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
