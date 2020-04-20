(ns firn.core
  (:require [clojure.java.io :as io]
            [cli-matic.core :refer [run-cmd]]
            [firn.build :as build])
  (:gen-class))

(defn native-image? []
  (and (= "Substrate VM" (System/getProperty "java.vm.name"))
       (= "runtime" (System/getProperty "org.graalvm.nativeimage.imagecode"))))

(defn init!
  "When firn is run as a native image, move the dependencies (the parser bin)
  to the home directory. Not ideal, but this is the best we can do for now!"
  []
  (when (native-image?)
    (let [home (System/getProperty "user.home")
          lib-dir (io/file home ".firn")]
      (.mkdirs lib-dir)
      (doseq [lib-name ["libmylib.dylib" "libmylib.so"]]
        (when-let [resource (io/resource lib-name)]
          (let [lib-file (io/file lib-dir lib-name)]
            (io/copy (io/input-stream resource) lib-file))))
      (System/setProperty "java.library.path" (.getPath lib-dir)))))


;; CLI commands
(def CONFIGURATION
  {:app         {:command     "firn"
                 :description "A static-site generator for org-mode."
                 :version     "0.0.1"}

   :commands    [{:command     "build"
                  :description "Builds a static site in a directory with org files."
                  ;; :opts        [{:option "path" :short "p"  :as "Specify path to content" :type :string :default ""}]
                  :runs        build/all-files}
                 {:command     "new"
                  :description "Scaffolds files and folders needed to start a new site."
                  :opts        []
                  :runs        build/new-site}]})


(defn -main
  "Parsed command line arguments and runs corresponding functions."
  [& args]
  (init!)
  (clojure.lang.RT/loadLibrary "mylib")
  (run-cmd args CONFIGURATION))

;; (defn -main
;;   [& args]
;;   (case (first args)
;;     "new"   (build/new-site)
;;     "build" (build/all-files)
;;     (prn "Please pass the command 'new' or 'build'")))
