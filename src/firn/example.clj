(ns firn.example)

(def ex
  {:type "document",
   :children
   [{:type "section",
     :children
     [{:type "keyword",
       :key "TITLE",
       :value "Tenses"}
      {:type "keyword",
       :key "DATE_CREATED",
       :value "2020-02-28--08-31"}
      {:type "keyword",
       :key "FILE_UNDER",
       :value "research"}]}
    {:type "headline",
     :level 1,
     :children
     [{:type "title",
       :level 1,
       :raw "Meta",
       :properties {:slug "?", :file_under "?"},
       :children [{:type "text", :value "Meta"}]}
      {:type "section",
       :children
       [{:type "drawer", :name "LOGBOOK"}]}]}
    {:type "headline",
     :level 1,
     :children
     [{:type "title",
       :level 1,
       :raw "Resources",
       :children
       [{:type "text", :value "Resources"}]}]}
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
       [{:type "paragraph",
         :children
         [{:type "text",
           :value "It has taken me learning a "}
          {:type "link",
           :path
           "file:research_learning_french_2020-02-24--21-20.org",
           :desc "new language�"}
          {:type "text",
           :value
           " that I have realized I don't know what\r\nmost of the words surrounding describing tenses are in my native language.\r"}]}
        {:type "list",
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
               "The simple past in english is used to show a completed action that took place\r"}]}]}]}
        {:type "paragraph",
         :children
         [{:type "text",
           :value "at a time in the past. "}
          {:type "underline",
           :children
           [{:type "text", :value "Example"}]}
          {:type "text", :value ": I "}
          {:type "bold",
           :children
           [{:type "text", :value "lost"}]}
          {:type "text",
           :value " my wallet on Sunday, so I "}
          {:type "bold",
           :children
           [{:type "text", :value "bought"}]}
          {:type "text",
           :value
           "  a\r\nnew one yesterdray.\r"}]}
        {:type "list",
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
               "The present perfect is used with has/have and a past particible. In "}
              {:type "link",
               :path
               "file:research_learning_french_2020-02-24--21-20.org",
               :desc "French"}
              {:type "text",
               :value
               ",\r\n  this is similarly done using "}
              {:type "code", :value "avoir"}
              {:type "text", :value ".\r"}]}]}]}
        {:type "quote-block",
         :parameters nil,
         :children
         [{:type "paragraph",
           :children
           [{:type "text", :value "fail\r"}]}]}
        {:type "source-block",
         :contents
         "function sum (x, y) {\r\n    return 1 + y\r\n}\r\n",
         :language "js",
         :arguments ""}]}]}]})

(def simple
  {:type "document",
   :children
   [{:type "headline",
     :level 1,
     :children
     [{:type "title",
       :level 1,
       :raw "A heading",
       :children
       [{:type "text", :value "A heading"}]}
      {:type "section",
       :children
       [{:type "paragraph",
         :children
         [{:type "text", :value "some "}
          {:type "code", :value "content"}
          {:type "text", :value ".\r"}]}
        {:type "paragraph",
         :children
         [{:type "text",
           :value "and other content.\r"}]}]}]}
    {:type "headline",
     :level 1,
     :children
     [{:type "title",
       :level 1,
       :raw "Heading two.",
       :children
       [{:type "text", :value "Heading two."}]}
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
               :value "list 1\r"}]}]}
          {:type "list-item",
           :bullet "- ",
           :children
           [{:type "paragraph",
             :children
             [{:type "text",
               :value "list 2\r"}]}]}]}]}]}]}
  )
