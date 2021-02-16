(defproject firn "0.0.5-SNAPSHOT"
  :description "Static Site Generator for Org Mode"
  :url "https://github.com/theiceshelf/firn"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[borkdude/sci "0.1.1-alpha.9"]
                 [cheshire "5.10.0"]
                 [clj-rss "0.2.5"]
                 [hiccup "1.0.5"]
                 [hiccup-find "1.0.0"]
                 [http-kit "2.3.0"]
                 ;; [juxt/dirwatch "0.2.5"] ;; vendored
                 [me.raynes/fs "1.4.6"]
                 [org.clojure/tools.cli "1.0.194"]
                 [ring "1.8.0"]
                 [mount "0.1.16"]
                 [org.clojure/clojure "1.10.2-alpha2"]
                 [org.clojure/data.priority-map "1.0.0"]

                 [ring/ring-defaults "0.3.2"]]

  :jvm-opts ["-Djava.library.path=./resources"]
  :java-source-paths ["src-java"]
  :plugins [[lein-cljfmt "0.6.7"]
            [lein-cloverage "1.1.2"]]
  :resources-paths ["resources"]
  :main firn.core
  :profiles {:uberjar {:aot :all
                       :main firn.core
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"
                                  "-Dclojure.spec.skip-macros=true"]}}
  :repl-options {:init-ns firn.core})
