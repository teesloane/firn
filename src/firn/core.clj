(ns firn.core
  (:require [firn.build :as build]
            [cli-matic.core :refer [run-cmd]])
  (:gen-class))

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
                  :runs        build/new-site}
                 ]})


(defn -main
  "Parsed command line arguments and runs corresponding functions."
  [& args]
  (run-cmd args CONFIGURATION))
