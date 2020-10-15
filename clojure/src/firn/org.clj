(ns firn.org
  "Functions for managing org-related things.
  Most of these functions are for operating on EDN-fied org-file
  Which are created by the rust binary."
  (:require [clojure.java.shell :as sh]
            [clojure.string :as s]
            [firn.util :as u]
            [cheshire.core :as json])
  (:import iceshelf.clojure.rust.ClojureRust))

(defn parse!
  "Parse the org-mode file-string.
  NOTE: When developing with a REPL, this shells out to the rust bin.
  When compiled to a native image, it uses JNI to talk to the rust .dylib."
  [file-str]
  (if (u/native-image?)
    (ClojureRust/getFreeMemory file-str) ;; TODO: get free memory should be renamed to "parse-org" or something else.
    (let [parser   (str (u/get-cwd) "/resources/parser")
          stripped (s/trim-newline file-str)
          res      (sh/sh parser stripped)]
      (if-not (= (res :exit) 0)
        (prn "Orgize failed to parse file." stripped res)
        (res :out)))))

(defn parse-dev!
  "Parses a string and returns it as edn. Useful for "
  [s]
  (let [parser   (str (u/get-cwd) "/resources/parser")
        stripped (s/trim-newline s)
        res      (sh/sh parser stripped)]
    (if-not (= (res :exit) 0)
      (prn "Orgize failed to parse file." stripped res)
      (json/parse-string (res :out) true))))

;; -- Headlines

(defn get-headline-tags
  "Takes a headline structure and returns it's tags."
  [hl]
  (-> hl :children first :tags))

(defn headline-exported?
  [v]
  (u/in? (get-headline-tags v) "noexport"))

(defn get-headline-helper
  "Sanitizes a heading of links and just returns text.
  Necessary because org leafs of :type `link` have a `:desc` and not a `:value`

  Turns: `* This is my [[Headline]] with a link!` -> `This is my Headline with a link!`

  UGH. this is a mess of a function, and should be refactored. Basically:
  - Loop through the children that represent a title.
  - get the values out and trim them in case there is whitespace
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
                         "text"     (get-trimmed-val child :value)
                         "link"     (get-trimmed-val child :desc)
                         "code"     (get-trimmed-val child :value)
                         "verbatim" (get-trimmed-val child :value)
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

(defn make-headline-anchor
  "Takes a headline data structure and returns the id 'anchored' for slugifying"
  [node]
  (-> node get-headline-helper u/clean-anchor))

;; -- Dates / Time

(defn parsed-org-date->unix-time
  "Converts the parsed org date (ex: [2020-04-27 Mon 15:39] -> 1588003740000)
  and turns it into a unix timestamp."
  [{:keys [year month day hour minute]} file-name]

  (let [pod->str    (str year "-" month "-" day "T" hour ":" minute ":00.000-0000")
        sdf         (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSSZ")]
    (try
      (.getTime (.parse sdf pod->str))
      (catch Exception e
        (u/print-err! :warning  (str "Failed to parse the logbook for file:" "<<" file-name ">>" "\nThe logbook may be incorrectly formatted.\nError value:" e))
        "???"))))

;; -- Links
(defn get-link-parts
  "Converts `file:my_link.org` -> data of it's representative parts.
  file:my_link.org -> {:anchor nil :slug 'my_link'} "
  [org-link]
  (let [regex       #"(file:)(.*)\.(org)(\:\:\*.+)?"
        res          (re-matches regex org-link)
        anchor-link  (last res)
        anchor-link  (when anchor-link (-> res last u/clean-anchor))
        file-slug   (nth res 2)]
    {:anchor anchor-link :slug file-slug}))

;; -- stats --

(defn- find-day-to-update
  [calendar-year log-entry]
  (let [{:keys [day month year]} (log-entry :start)
        logbook-date             (u/date-str (u/date-make year month day))]
    (u/find-index-of #(= (% :date-str) logbook-date) calendar-year)))

(defn- update-logbook-day
  "Updates a day in a calander from build-year with logbook data."
  [{:keys [duration] :as log-entry}]
  (fn [{:keys [log-count logs-raw log-sum] :as cal-day}]
    (let [log-count (inc log-count)
          logs-raw  (conj logs-raw log-entry)
          log-sum   (u/timestr->add-time log-sum duration)]
      (merge
       cal-day
       {:log-count log-count
        :logs-raw  logs-raw
        :log-sum   log-sum
        :hour-sum  (u/timestr->hour-float log-sum)}))))

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
                              (assoc output log-year (u/build-year log-year))) ; make year if there isn't one already.
            day-to-update (find-day-to-update (get output log-year) x)
            output        (update-in output [log-year day-to-update] (update-logbook-day x))]
        (recur xs output)))))

;; Rendered charts:

;; TODO This should be in markup, as it's spitting out html
(defn poly-line
  "Takes a logbook, formats it so that it can be plotted along a polyline."
  ([logbook]
   (poly-line logbook {}))
  ([logbook
    {:keys [width height stroke stroke-width]
     :or   {width 365 height 100 stroke "#0074d9" stroke-width 1}}]
   [:div
    (for [[year year-of-logs] (logbook-year-stats logbook)
          :let [; max-log     (apply max-key :hour-sum year-of-logs) ;; Don't need this yet.
                ;; This should be measured against the height and whatever the max-log is.
                g-multiplier (/ height 8) ;; 8 - max hours we expect someone to log in a day
                fmt-points  #(str %1 "," (* g-multiplier (%2 :hour-sum)))
                points      (s/join " " (->> year-of-logs (map-indexed fmt-points)))]]

      [:div
       [:h5.firn-headline.firn-headline-5 year]
       [:svg {:viewbox (format "0 0 %s %s" width height)
              :class   "chart"}
        [:g {:transform (format "translate(0, %s) scale(1, -1)", (- height (* stroke-width 1.25)))}
         [:polyline {:fill         "none"
                     :stroke       stroke
                     :stroke-width "1"
                     :points points}]]]])]))
