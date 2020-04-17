(defproject firn "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [cli-matic "0.3.11"]
                 [borkdude/sci "0.0.13-alpha.14"]
                 [me.raynes/fs "1.4.6"]]

  :jvm-opts ["-Djava.library.path=./resources"]
  :java-source-paths ["src-java"]
  :resources-paths ["resources"]
  :main firn.core
  :profiles {:uberjar {:aot :all
                       :main firn.core
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"
                                  "-Dclojure.spec.skip-macros=true"]}}
  :repl-options {:init-ns firn.core})
