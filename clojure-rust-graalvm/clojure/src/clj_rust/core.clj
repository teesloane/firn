(ns clj-rust.core
  (:require [clojure.java.io :as io])
  (:import [iceshelf.clojure.rust ClojureRust])
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
  [& [unit]]
  (init!)
  (clojure.lang.RT/loadLibrary "mylib");
  (if-not (contains? #{"byte" "megabyte" "gigabyte"} unit)
    (binding [*out* *err*]
      (println "Expected unit argument: byte, megabyte or gigabyte.")
      (when unit (println "Got:" unit)))
    (prn {:memory/free [(keyword unit) (ClojureRust/getFreeMemory unit)]})))
