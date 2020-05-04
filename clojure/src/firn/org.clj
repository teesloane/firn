(ns firn.org
  "Functions for managing org-related things.
  Most of these functions are for operating on EDN-fied org-file
  Which are created by the rust binary."
  (:require [clojure.string :as s]
            [firn.util :as u]
            [clojure.java.shell :as sh])
  (:import [iceshelf.clojure.rust ClojureRust]))

(defn parse!
  "Parse the org-mode file-string.
  NOTE: When developing with a REPL, this shells out to the rust bin.
  When compiled to a native image, it uses JNI to talk to the rust .dylib."
  [file-str]
  (if (u/native-image?)
    (ClojureRust/getFreeMemory file-str)
    (let [parser (str (u/get-cwd) "/resources/parser")
          res    (sh/sh parser (s/trim-newline file-str))]
      (if-not (= (res :exit) 0)
        (prn "Orgize failed to parse file." file-str res)
        (res :out)))))

(defn- get-headline-helper
  "Sanitizes a heading of links and just returns text.
  Necessary because org leafs of :type `link` have a `:desc` and not a `:value`

  Turns: `* This is my [[Headline]] with a link!` -> `This is my Headline with a link!`

  UGH. this is a mess of a function, and should be refactored. Basically:
  - Loop through the children that represent a title.
  - get the values out and trim them incase there is whitespace
  (between say, a cookie or a priority, or empty text)
  - Then, filter out all empty strings
  - Then join them together.

  All in all, this is supposed to remove the dynamic elements of a heading so user's don't have to
  search headlines that have percentages, or priorities, etc."
  [headline]
  (let [title-children  (-> headline :children first :children)
        get-trimmed-val #(let [trimmed-val (s/trim (get %1 %2 ""))] ;; NOTE: not sure if should default to ""
                           (if (empty? trimmed-val) "" trimmed-val))]
    (s/join " "
            (filter not-empty
                    (for [child title-children]
                      (s/trim
                       (case (:type child)
                         "text" (get-trimmed-val child :value)
                         "link" (get-trimmed-val child :desc)
                         "")))))))

(defn get-headline
  "Fetches a headline from an org-mode tree."
  [tree name]
  (->> (tree-seq map? :children tree)
       (filter #(and (= "headline" (:type %))
                     (= name (get-headline-helper %))))
       (first)))

(defn get-headline-content
  "Same as get-headline, but removes the first child :title)."
  [tree name]
  (let [headline (get-headline tree name)]
    (update headline :children (fn [d] (filter #(not= (:type %) "title") d)))))

(defn parsed-org-date->unix-time
  "Converts the parsed org date (ex: [2020-04-27 Mon 15:39] -> 1588003740000)
  and turns it into a unix timestamp."
  [{:keys [year month day hour minute] :as processed-org-date-time}
   {:keys [name] :as file}]
  (let [pod->str    (str year "-" month "-" day "T" hour ":" minute ":00.000-0000")
        sdf         (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSSZ")]
    (try
      (.getTime (.parse sdf pod->str))
      ;; TODO: This should probably run System.exit / use print-err!
      (catch Exception e
        (println (str "\nFailed to parse the logbook for :" "<<"name">>" "\nThe logbook may be incorrectly formatted.\nError value:" e))))))
