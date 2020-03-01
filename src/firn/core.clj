(ns firn.core
  (:require [clojure.java.io :as io])
  (:gen-class))

;; Files Reading

(defn get-files
  "Returns a list of files as Java objects.
  Filters out all non `.org` files.
  TODO: handle empty folder; handle custom folder, handle none found.
  "
  []
  (->> "/Users/tees/Dropbox/wiki/"
       clojure.java.io/file
       file-seq
       (filter #(.isFile %))
       (filter (fn [file] (-> file .getName (.endsWith ".org"))))))


(defn read-files
  "Takes a list of files and reads them, eventually parsing them."
  [f]
  (slurp f))


(defn write-files
  "Takes read and parsed content files and writes them to output."
  [f]
  (spit "out.txt" f))


(defn compile
  []
  (-> (get-files)
      (first)
      (read-files)
      (write-files)))

(compile)

;; File Writing

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Reading Files...")
  (compile)
  (println "Created Files."))
