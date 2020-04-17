(ns firn.core
  (:require [clojure.java.io :as io]
            [firn.build :as build])
  (:gen-class))

(defn native-image? []
  (and (= "Substrate VM" (System/getProperty "java.vm.name"))
       (= "runtime" (System/getProperty "org.graalvm.nativeimage.imagecode"))))

(defn init! []
  (when (native-image?)
    (let [home (System/getProperty "user.home")
          lib-dir (io/file home ".clojure_rust")]
      (.mkdirs lib-dir)
      (doseq [lib-name ["libmylib.dylib" "libmylib.so"]]
        (when-let [resource (io/resource lib-name)]
          (let [lib-file (io/file lib-dir lib-name)]
            (io/copy (io/input-stream resource) lib-file))))
      (System/setProperty "java.library.path" (.getPath lib-dir)))))

(defn -main
  [& args]
  (init!)
  (clojure.lang.RT/loadLibrary "mylib")
  (case (first args)
    "new"   (build/new-site)
    "build" (build/all-files)
    (prn "Please pass the command 'new' or 'build'")))
