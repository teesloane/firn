(ns firn.org
  "Functions for managing org-related things.
  Most of these functions are for operating on EDN-fied org-file
  Which are created by the rust binary."
  (:require [clojure.java.shell :as sh]
            [clojure.string :as s]
            [firn.util :as u])
  (:import iceshelf.clojure.rust.ClojureRust))

(defn parse!
  "Parse the org-mode file-string.
  TODO: get new binary that can parse footnotes
  NOTE: When developing with a REPL, this shells out to the rust bin.
  When compiled to a native image, it uses JNI to talk to the rust .dylib."
  [file-str]
  (if (u/native-image?)
    (ClojureRust/getFreeMemory file-str)
    (let [parser   (str (u/get-cwd) "/resources/parser")
          stripped (s/trim-newline file-str)
          res      (sh/sh parser stripped)]
      (if-not (= (res :exit) 0)
        (prn "Orgize failed to parse file." stripped res)
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
  [{:keys [year month day hour minute]}
   {:keys [name] :as file}]
  (let [pod->str    (str year "-" month "-" day "T" hour ":" minute ":00.000-0000")
        sdf         (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSSZ")]
    (try
      (.getTime (.parse sdf pod->str))
      (catch Exception e
        (u/print-err! :warning  (str "Failed to parse the logbook for file:" "<<" name ">>" "\nThe logbook may be incorrectly formatted.\nError value:" e))
        "???"))))

;; -- stats --

;; Days of each month
(def cal-schema
  [[1 31] [2 28] [3 31] [4 30] [5 31] [6 30]
   [7 31] [8 31] [9 30] [10 31] [11 30] [12 31]])

(defn- build-year
  "constructs a list of 365 days.
  used to push logbook vals into the list for charting."
  [year]
  (vec
   (flatten
    (for [month cal-schema
          :let  [curr-month    (first month)
                 days-in-month (range 1 (+ 1 (second month)))]]

      (for [day days-in-month]
        ;; construct a day; this gets modified later in logbook-year-stats.
        {:day         day
         :month       curr-month
         :year        year
         :hours       0
         :log-count   0
         :logs-raw    []
         :log-sum    "00:00"})))))

(defn- update-logbook-day
  "Updates a day in a calander from build-year with logbook data."
  [log-entry]
  #(-> %
       (assoc :log-count (inc (% :log-count)))
       (assoc :year      (-> log-entry :start :year))
       (assoc :logs-raw  (conj (% :logs-raw) log-entry))
       (assoc :log-sum   (u/timestr->add-time (% :log-sum) (log-entry :duration)))
       (assoc :hours     (u/timestr->hours (% :log-sum)))))

(defn logbook-year-stats
  "Takes a logbook and pushes it's data into a year calendar."
  [logbook]
  (loop [logbook logbook
         output   {}]
    (if (empty? logbook)
      output
      (let [x             (first logbook)
            xs            (rest logbook)
            log-year      (-> x :start :year)
            day-to-update (- (-> x :start :day) 1) ; account for 0 based index.
            ;; create the "year" if it doesnt exist yet.
            output        (if (contains? output log-year)
                            output
                            (assoc output log-year (build-year log-year))) ;;make year if there isn't one already.
            output      (update-in output [log-year day-to-update] (update-logbook-day x))]
        (recur xs output)))))

(defn make-poly-line
  [logbook]
  [:div
   (for [year (logbook-year-stats logbook)
         :let [poly-line-pts (map-indexed (fn [idx item] [idx (get item :hours 0)]) year)
               points-as-str (apply str (map #(str "" (first %) "," (second %) " ") poly-line-pts))]]

     [:svg {:viewbox "0 0 500 100", :class "chart"}
      [:polyline {:fill         "none",
                  :stroke       "#0074d9",
                  :stroke-width "3",
                  :points       points-as-str}]])])


;; sample ground.
;; (def sample-logbook
;;   [
;;    {:type     "clock",
;;     :start    {:year 2020, :month 3, :day 31, :dayname "Tue", :hour 19, :minute 36},
;;     :end      {:year 2020, :month 3, :day 31, :dayname "Tue", :hour 19, :minute 46},
;;     :duration "0:10"}
;;    {:type     "clock",
;;     :start    {:year 2020, :month 3, :day 31, :dayname "Tue", :hour 13, :minute 15},
;;     :end      {:year 2020, :month 3, :day 31, :dayname "Tue", :hour 17, :minute 36},
;;     :duration "4:21"}
;;    {:type     "clock",
;;     :start    {:year 2020, :month 3, :day 31, :dayname "Tue", :hour 10, :minute 55},
;;     :end      {:year 2020, :month 3, :day 31, :dayname "Tue", :hour 12, :minute 17},
;;     :duration "1:22"}
;;    {:type     "clock",
;;     :start    {:year 2020, :month 3, :day 30, :dayname "Mon", :hour 14, :minute 14},
;;     :end      {:year 2020, :month 3, :day 30, :dayname "Mon", :hour 14, :minute 41},
;;     :duration "0:27"}
;;    {:type     "clock",
;;     :start    {:year 2020, :month 3, :day 29, :dayname "Sun", :hour 17, :minute 8},
;;     :end      {:year 2020, :month 3, :day 29, :dayname "Sun", :hour 20, :minute 31},
;;     :duration "3:23"}
;;    {:type     "clock",
;;     :start    {:year 2019, :month 1, :day 1, :dayname "Sat", :hour 15, :minute 45},
;;     :end      {:year 2019, :month 1, :day 1, :dayname "Sat", :hour 18, :minute 29},
;;     :duration "2:44"}])
;; (make-poly-line sample-logbook)
