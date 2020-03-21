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
                  :opts        [{:option "path" :short "p"  :as "Specify path to content" :type :string :default ""}]
                  :runs        build/all-files}

                 ]})


(defn -main
  "Takes a link to a directory and runs on the list of org files."
  [& args]
  (run-cmd args CONFIGURATION))
;; ;; (let [files-dir (first args)] ;; TODO setup cli args.
;; ;;   (build/all-files files-dir)

;; #_(If config/dev?
;;       (System/exit 0))))

;; (-main)
