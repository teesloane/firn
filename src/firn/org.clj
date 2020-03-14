(ns firn.org
  "Functions for managing org-related things.
  Most of these functions are for operating on EDN-fied org-file
  Which are created by the rust binary."
  (:require [hiccup.core :as h]
            [clojure.string :as s]))


(defn get-1st-level-headline-by-name
  "Takes org tree and filters down to a headline + it's contents
  Example: `(get-1st-level-headline-by-name 'Meta' org-edn)`
  FIXME: Needs to handle cases where nothing is found.
  FIXME: Make this for finding ANY headline."
  [name org-edn]
  (->> org-edn
       (map (fn [v]
              (when (= (:type v) "headline") v)))
       ;; get level 1 headings
       (filter #(= (get % :level nil) 1))
       ;; get at the (non-raw) heading if it matches `name`
       (filter #(= (-> % :children first :children first :value s/trim) name))
       (remove nil?)
       (first)))


(def x
  {:type "document",
   :pre_blank 0,
   :children
   [{:type "section",
     :children
     [{:type "keyword", :key "TITLE", :value "Firn", :post_blank 0}
      {:type "keyword",
       :key "DATE_CREATED",
       :value "2020-03-01--09-53",
       :post_blank 0}
      {:type "keyword", :key "STATUS", :value "active", :post_blank 0}
      {:type "keyword", :key "FILE_UNDER", :value "project", :post_blank 0}
      {:type "keyword", :key "LAYOUT", :value "project", :post_blank 1}]}
    {:type "headline",
     :level 1,
     :children
     [{:type "title",
       :level 1,
       :raw "Meta",
       :properties
       {:state "active",
        :date_started "<2020-03-01 Sun>",
        :file_under "project",
        :date_completed "?",
        :intent "Wiki",
        :slug "firn",
        :links "?"},
       :post_blank 0,
       :children [{:type "text", :value "Meta"}]}
      {:type "section",
       :children
       [{:type "drawer",
         :name "LOGBOOK",
         :pre_blank 0,
         :post_blank 0,
         :children
         [{:type "clock",
           :start
           {:year 2020, :month 3, :day 11, :dayname "Wed", :hour 9, :minute 53},
           :end
           {:year 2020, :month 3, :day 11, :dayname "Wed", :hour 10, :minute 40},
           :duration "0:47",
           :post_blank 0}
          {:type "clock",
           :start
           {:year 2020, :month 3, :day 8, :dayname "Sun", :hour 17, :minute 51},
           :end
           {:year 2020, :month 3, :day 8, :dayname "Sun", :hour 18, :minute 0},
           :duration "0:09",
           :post_blank 0}
          {:type "clock",
           :start
           {:year 2020, :month 3, :day 7, :dayname "Sat", :hour 8, :minute 26},
           :end
           {:year 2020, :month 3, :day 7, :dayname "Sat", :hour 9, :minute 43},
           :duration "1:17",
           :post_blank 0}
          {:type "clock",
           :start
           {:year 2020, :month 3, :day 6, :dayname "Fri", :hour 16, :minute 17},
           :end
           {:year 2020, :month 3, :day 6, :dayname "Fri", :hour 18, :minute 26},
           :duration "2:09",
           :post_blank 0}
          {:type "clock",
           :start
           {:year 2020, :month 3, :day 5, :dayname "Thu", :hour 21, :minute 3},
           :end
           {:year 2020, :month 3, :day 5, :dayname "Thu", :hour 21, :minute 50},
           :duration "0:47",
           :post_blank 0}
          {:type "clock",
           :start
           {:year 2020, :month 3, :day 5, :dayname "Thu", :hour 13, :minute 5},
           :end
           {:year 2020, :month 3, :day 5, :dayname "Thu", :hour 13, :minute 35},
           :duration "0:30",
           :post_blank 0}
          {:type "clock",
           :start
           {:year 2020, :month 3, :day 4, :dayname "Wed", :hour 21, :minute 10},
           :end
           {:year 2020, :month 3, :day 4, :dayname "Wed", :hour 21, :minute 28},
           :duration "0:18",
           :post_blank 0}
          {:type "clock",
           :start
           {:year 2020, :month 3, :day 4, :dayname "Wed", :hour 13, :minute 31},
           :end
           {:year 2020, :month 3, :day 4, :dayname "Wed", :hour 15, :minute 31},
           :duration "2:00",
           :post_blank 0}
          {:type "clock",
           :start
           {:year 2020, :month 3, :day 3, :dayname "Tue", :hour 11, :minute 34},
           :end
           {:year 2020, :month 3, :day 3, :dayname "Tue", :hour 15, :minute 55},
           :duration "4:21",
           :post_blank 0}
          {:type "clock",
           :start
           {:year 2020, :month 3, :day 2, :dayname "Mon", :hour 13, :minute 11},
           :end
           {:year 2020, :month 3, :day 2, :dayname "Mon", :hour 17, :minute 45},
           :duration "4:34",
           :post_blank 0}
          {:type "clock",
           :start
           {:year 2020, :month 3, :day 1, :dayname "Sun", :hour 17, :minute 34},
           :end
           {:year 2020, :month 3, :day 1, :dayname "Sun", :hour 18, :minute 9},
           :duration "0:35",
           :post_blank 0}]}]}]}
    {:type "headline",
     :level 1,
     :children
     [{:type "title",
       :level 1,
       :raw "Notes",
       :post_blank 0,
       :children [{:type "text", :value "Notes"}]}
      {:type "section",
       :children
       [{:type "quote-block",
         :pre_blank 0,
         :post_blank 1,
         :children
         [{:type "paragraph",
           :post_blank 1,
           :children
           [{:type "text",
             :value
             "Firn is found under the snow that accumulates at the head of a glacier. It is formed under the pressure of overlying snow by the processes of compaction, recrystallization, localized melting, and the crushing of individual snowflakes."}]}
          {:type "list",
           :indent 0,
           :ordered false,
           :post_blank 0,
           :children
           [{:type "list-item",
             :bullet "- ",
             :indent 0,
             :ordered false,
             :children
             [{:type "paragraph",
               :post_blank 0,
               :children
               [{:type "link",
                 :path "https://www.britannica.com/science/firn",
                 :desc "Brittanica: Firn"}]}]}]}]}
        {:type "paragraph",
         :post_blank 1,
         :children
         [{:type "text", :value "Cumulative wiki engine built with"}
          {:type "link", :path "file:org-mode.org", :desc " Org Modeº"}
          {:type "text", :value " and "}
          {:type "link", :path "file:clojure.org", :desc "Clojureº"}
          {:type "text", :value "."}]}
        {:type "paragraph",
         :post_blank 0,
         :children
         [{:type "text", :value "Primarily catalogues "}
          {:type "link", :path "file:projects.org", :desc "Projectsº"}
          {:type "text", :value " and "}
          {:type "link", :path "file:research.org", :desc "Researchº"}
          {:type "text",}]}]}]}]})


(defn get-headline
  [tree name]
  (let [pred #(and (= "headline" (:type %))
                   (= name (-> % :children first :raw)))]
    (->> (tree-seq map? :children x)
         (filter pred)
         (first))))


(get-headline x "Meta")
