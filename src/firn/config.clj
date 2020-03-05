(ns firn.config)

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
               :value "list 2\r"}]}]}]}]}]}
    {:type "headline",
     :level 1,
     :children
     [{:type "title",
       :level 1,
       :raw "Headline One!",
       :properties {:shoes "yes!"},
       :children
       [{:type "text",
         :value "Headline One!"}]}]}]})

(def table-ex
  {:type "document",
   :children
   [{:type "section",
     :children
     [{:type "table",
       :table_type "org",
       :tblfm nil,
       :children
       [{:type "table-row",
         :table_row_type "standard",
         :children
         [{:type "table-cell",
           :children
           [{:type "text", :value "Expression"}]}
          {:type "table-cell",
           :children
           [{:type "text", :value "Meaning"}]}]}
        {:type "table-row",
         :table_row_type "rule"}
        {:type "table-row",
         :table_row_type "standard",
         :children
         [{:type "table-cell",
           :children
           [{:type "text",
             :value
             "Je suis soud comme un pot"}]}
          {:type "table-cell",
           :children
           [{:type "text",
             :value
             "I'm as deaf as a pot."}]}]}]}]}]})



(defn default
  [files-dir]
  {:out-dir   (str files-dir "_site/") ; where files get published
   :files-dir files-dir                ; where org content lives.
   :org-files nil                      ; a list of org files, added to as files get converted.
   ;current file being operated on.
   :curr-file nil})
