(ns firn.org
  "Functions for managing org-related things.
  Most of these functions are for operating on EDN-fied org-file
  Which are created by the rust binary."
  (:require [clojure.java.shell :as sh]
            [clojure.string :as s]
            [tick.alpha.api :as t]
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

;; TODO - test me; move to utils.year.
(defn- build-year-tick
  [year]
  (let [interval           (t/bounds (t/year year))
        ;;This  creates a year of values that look like: #time/date-time "2019-01-01T00:00"
        date-times-of-year (t/range (t/beginning interval)
                                    (t/end interval)
                                    (t/new-period 1 :days))
        ;; but we just need dates : 2019-01-01 (not yet possible in tick?)
        dates-of-year      (map t/date date-times-of-year)
        build-days         #(hash-map
                             :date      %
                             :log-count 0
                             :logs-raw  []
                             :log-sum   "00:00"
                             :hour-sum   0)]

    (->> dates-of-year (map build-days) vec)))


;; TODO - move to util.s.
(defn find-index-of
  [pred sequence]
  (first (keep-indexed (fn [i x] (when (pred x) i))
                       sequence)))

(defn find-day-to-update
  [calendar-year log-entry]
  (let [{:keys [day month year]} (log-entry :start)
        logbook-date             (t/new-date year month day)]
    (find-index-of #(= (% :date) logbook-date) calendar-year)))

(defn- update-logbook-day
  "Updates a day in a calander from build-year with logbook data."
  [{:keys [duration] :as log-entry}]
  (fn [{:keys [log-count logs-raw log-sum day] :as cal-day}]
    (let [log-count (inc log-count)
          logs-raw  (conj logs-raw log-entry)
          log-sum   (u/timestr->add-time log-sum duration)]
      (merge
       cal-day
       {:log-count log-count
        :logs-raw  logs-raw
        :log-sum   log-sum
        :hour-sum  (u/timestr->hour-float log-sum)}))))


;; TODO TEST ME.
(defn logbook-year-stats
  "Takes a logbook and pushes it's data into a year calendar.
  Returns a map that looks like:
  2020 = [ { :day 1, ... } { :day 2, ... } { :day 3, ... } { :day 4, ... } ... ]
  2019 = [ { :day 1, ... } { :day 2, ... } { :day 3, ... } { :day 4, ... } ... ]
  "
  [logbook]
  (loop [logbook logbook
         output  {}]
    (if (empty? logbook)
      output
      (let [x             (first logbook)
            xs            (rest logbook)
            log-year      (-> x :start :year)
            output        (if (contains? output log-year) output
                              (assoc output log-year (build-year-tick log-year))) ; make year if there isn't one already.
            day-to-update (find-day-to-update (get output log-year) x)
            output        (update-in output [log-year day-to-update] (update-logbook-day x))]
        (recur xs output)))))

;; Rendered charts:

(defn poly-line
  "Takes a logbook, formats it so that it can be plotted along a polyline."
  ([logbook]
   (poly-line logbook {}))
  ([logbook
    {:keys [width height stroke]
     :or   {width 365 height 100 stroke "#0074d9"}
     :as   opts}]
   [:div
    (for [[year year-of-logs] (logbook-year-stats logbook)
          :let [
                max-log     (apply max-key :hour-sum year-of-logs) ;; Don't need this yet.
                ;; This should be measured against the height and whatever the max-log is.
                g-multiplier (/ height 8) ;; 8 - max hours we expect someone to log in a day
                fmt-points  #(str %1 "," (* g-multiplier (%2 :hour-sum)))
                points      (s/join " " (->> year-of-logs (map-indexed fmt-points)))]]


      [:div
       [:h5.firn_heading.firn_heading-5 year]
       [:svg {:viewbox (format "0 0 %s %s" width height),
              :class   "chart"}
        [:g {:transform (format "translate(0, %s) scale(1, -1)", height)}
         [:polyline {:fill         "none",
                     :stroke       stroke,
                     :stroke-width "1",
                     :points points}]]]])]))

