(ns firn.example)

(def ex
  {:type "document",
   :children
   [{:type "section",
     :children
     [{:type "keyword",
       :key "TITLE",
       :value "Firn"}
      {:type "keyword",
       :key "DATE_CREATED",
       :value "2020-03-01--09-53"}
      {:type "keyword",
       :key "STATUS",
       :value "active"}
      {:type "keyword",
       :key "FILE_UNDER",
       :value "project"}]}
    {:type "headline",
     :level 1,
     :children
     [{:type "title",
       :level 1,
       :raw "Meta",
       :properties
       {:state "active",
        :links "?",
        :slug "firn",
        :date_completed "?",
        :date_started "<2020-03-01 Sun>",
        :file_under "shit",
        :intent "Wiki"},
       :children [{:type "text", :value "Meta"}]}
      {:type "section",
       :children
       [{:type "drawer",
         :name "LOGBOOK",
         :children
         [{:type "clock",
           :start
           {:year 2020,
            :month 3,
            :day 2,
            :dayname "Mon",
            :hour 13,
            :minute 11}}
          {:type "clock",
           :start
           {:year 2020,
            :month 3,
            :day 1,
            :dayname "Sun",
            :hour 17,
            :minute 34},
           :end
           {:year 2020,
            :month 3,
            :day 1,
            :dayname "Sun",
            :hour 18,
            :minute 9},
           :duration "0:35"}]}]}
      {:type "headline",
       :level 2,
       :children
       [{:type "title",
         :level 2,
         :raw "Related",
         :children
         [{:type "text", :value "Related"}]}
        {:type "section",
         :children
         [{:type "list",
           :indent 0,
           :ordered false,
           :children
           [{:type "list-item",
             :bullet "- ",
             :children
             [{:type "paragraph",
               :children
               [{:type "link",
                 :path
                 "file:research_org_mode_2020-02-28--20-56.org",
                 :desc "Org Mode�"}
                {:type "text", :value "\r"}]}]}
            {:type "list-item",
             :bullet "- ",
             :children
             [{:type "paragraph",
               :children
               [{:type "link",
                 :path
                 "file:research_emacs_2020-02-28--19-31.org",
                 :desc "Emacs�"}
                {:type "text",
                 :value "\r"}]}]}]}]}]}]}
    {:type "headline",
     :level 1,
     :children
     [{:type "title",
       :level 1,
       :raw "Resources",
       :children
       [{:type "text", :value "Resources"}]}
      {:type "section",
       :children
       [{:type "list",
         :indent 0,
         :ordered false,
         :children
         [{:type "list-item",
           :bullet "- ",
           :children
           [{:type "paragraph",
             :children
             [{:type "link",
               :path
               "https://www.britannica.com/science/firn",
               :desc "Namesake"}
              {:type "text",
               :value "\r"}]}]}]}]}]}
    {:type "headline",
     :level 1,
     :children
     [{:type "title",
       :level 1,
       :raw "Tasks [33%]",
       :children
       [{:type "text", :value "Tasks "}
        {:type "cookie", :value "[33%]"}]}
      {:type "headline",
       :level 2,
       :children
       [{:type "title",
         :level 2,
         :keyword "TODO",
         :raw "MVP [50%]",
         :children
         [{:type "text", :value "MVP "}
          {:type "cookie", :value "[50%]"}]}
        {:type "headline",
         :level 3,
         :children
         [{:type "title",
           :level 3,
           :keyword "TODO",
           :raw "Setup Parser [75%]",
           :children
           [{:type "text",
             :value "Setup Parser "}
            {:type "cookie", :value "[75%]"}]}
          {:type "section",
           :children
           [{:type "list",
             :indent 0,
             :ordered false,
             :children
             [{:type "list-item",
               :bullet "- ",
               :children
               [{:type "paragraph",
                 :children
                 [{:type "text",
                   :value
                   "[X] Find a good org-parser.\r"}]}]}
              {:type "list-item",
               :bullet "- ",
               :children
               [{:type "paragraph",
                 :children
                 [{:type "text",
                   :value
                   "[X] Slurp single file\r"}]}]}
              {:type "list-item",
               :bullet "- ",
               :children
               [{:type "paragraph",
                 :children
                 [{:type "text",
                   :value "[X] Parse file\r"}]}]}
              {:type "list-item",
               :bullet "- ",
               :children
               [{:type "paragraph",
                 :children
                 [{:type "text",
                   :value
                   "[ ] Display one basic element (propertees?) with hiccup.\r"}]}]}]}]}]}
        {:type "headline",
         :level 3,
         :children
         [{:type "title",
           :level 3,
           :keyword "DONE",
           :raw
           "Render a single org file to html [100%]",
           :planning
           {:closed
            {:timestamp_type "inactive",
             :start
             {:year 2020,
              :month 3,
              :day 1,
              :dayname "Sun",
              :hour 17,
              :minute 36}}},
           :children
           [{:type "text",
             :value
             "Render a single org file to html "}
            {:type "cookie", :value "[100%]"}]}
          {:type "section",
           :children
           [{:type "list",
             :indent 0,
             :ordered false,
             :children
             [{:type "list-item",
               :bullet "- ",
               :children
               [{:type "paragraph",
                 :children
                 [{:type "text",
                   :value "[X] Slurp File\r"}]}]}
              {:type "list-item",
               :bullet "- ",
               :children
               [{:type "paragraph",
                 :children
                 [{:type "text",
                   :value
                   "[X] render it to file as is.\r"}]}]}]}]}]}]}]}
    {:type "headline",
     :level 1,
     :children
     [{:type "title",
       :level 1,
       :raw "Notes",
       :children
       [{:type "text", :value "Notes"}]}
      {:type "section",
       :children
       [{:type "quote-block",
         :parameters nil,
         :children
         [{:type "paragraph",
           :children
           [{:type "text",
             :value
             "Firn is found under the snow that accumulates at the head of a glacier. It is formed under the pressure of overlying snow by the processes of compaction, recrystallization, localized melting, and the crushing of individual snowflakes.\r"}]}
          {:type "list",
           :indent 0,
           :ordered false,
           :children
           [{:type "list-item",
             :bullet "- ",
             :children
             [{:type "paragraph",
               :children
               [{:type "link",
                 :path
                 "https://www.britannica.com/science/firn",
                 :desc "Brittanica: Firn"}
                {:type "text",
                 :value "\r"}]}]}]}]}
        {:type "paragraph",
         :children
         [{:type "text",
           :value
           "Cumulative wiki engine built with"}
          {:type "link",
           :path
           "file:research_org_mode_2020-02-28--20-56.org",
           :desc " Org Mode�"}
          {:type "text", :value " and "}
          {:type "link",
           :path
           "file:research_clojure_2020-02-28--19-15.org",
           :desc "Clojure�"}
          {:type "text", :value ".\r"}]}
        {:type "paragraph",
         :children
         [{:type "text",
           :value "Primarily catalogues "}
          {:type "link",
           :path
           "file:projects_2020-02-26--15-01.org",
           :desc "Projects�"}
          {:type "text", :value " and "}
          {:type "link",
           :path
           "file:research_2020-03-01--09-55.org",
           :desc "Research�"}
          {:type "text",
           :value
           " as well as other experiments,\r\nquotes, ideas &c."}]}]}]}]})


(def ex2
  {:type "document",
   :children
   [{:type "section",
     :children
     [{:type "paragraph",
       :children
       [{:type "text",
         :value "I'm some org mode\r"}
        {:type "text"
         :value "with some more text"}]}
      {:type "paragraph",
       :children
       [{:type "text",
         :value "im more org mode"}]}]}]})


(def ex3
  {:type "document",
   :children
   [{:type "section",
     :children
     [{:type "paragraph",
       :children
       [{:type "text", :value "Im some "}
        {:type "bold",
         :children
         [{:type "bold",
           :children
           [{:type "text", :value "org mode"}]}]}
        {:type "text", :value " oh yeah"}]}]}]})

(def ex4
  {:type "document",
   :children
   [{:type "section",
     :children
     [{:type "paragraph",
       :children
       [{:type "text", :value "Im some "}
        {:type "underline",
         :children
         [{:type "text", :value "org mode"}]}
        {:type "text", :value " oh yeah"}]}]}]})


(def ex5
  {:type "section",
   :children
   [{:type "list",
     :indent 0,
     :ordered false,
     :children
     [{:type "list-item",
       :bullet "- ",
       :children
       [{:type "paragraph",
         :children
         [{:type "text",
           :value
           "[X] Find a good org-parser.\r"}]}]}
      {:type "list-item",
       :bullet "- ",
       :children
       [{:type "paragraph",
         :children
         [{:type "text",
           :value
           "[X] Slurp single file\r"}]}]}
      {:type "list-item",
       :bullet "- ",
       :children
       [{:type "paragraph",
         :children
         [{:type "text",
           :value "[X] Parse file\r"}]}]}
      {:type "list-item",
       :bullet "- ",
       :children
       [{:type "paragraph",
         :children
         [{:type "text",
           :value
           "[ ] Display one basic element (propertees?) with hiccup.\r"}]}]}]}]})
