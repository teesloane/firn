(ns firn.core
  (:require [clojure.java.io :as io])
  (:gen-class))

;; Files Reading

(defn get-files
  "Returns a list of files as Java objects.
  Filters out all non `.org` files.
  FIXME: handle empty folder; handle custom folder, handle none found.
  "
  []
  (println "Getting org files...")
  (->> "/Users/tees/Dropbox/wiki/"
       clojure.java.io/file
       file-seq
       (filter #(.isFile %))
       (filter (fn [file] (-> file .getName (.endsWith ".org"))))))


(defn read-file
  "Takes a list of files and reads them, eventually parsing them."
  [file]
  (println "Reading File..." (.getName file))
  (slurp file))


(defn write-file
  "Takes read and parsed content files and writes them to output."
  [f]
  (println "Writing files...")
  (spit "out.txt" f))


(defn compile
  []
  (let [files (get-files)
        files [(first files)]] ;; remove this when ready.
    (doseq [f files]
      (-> f
          (read-file)
          (write-file)))))

;; (compile)

;; File Writing

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (compile))
