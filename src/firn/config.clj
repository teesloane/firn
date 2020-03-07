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


(def curr-file
  {:name     nil
   :original nil      ; the file as as javaFile object.
   :keywords nil      ; list of keywords at top of org file: #+TITLE:, #+CATEGORY, etc.
   :as-json  nil      ; The org file, read as json and spat out by the rust binary.
   :as-edn   nil      ; JSON of org file -> converted to a map.
   :as-html  nil})    ; the html output

(def starting-config
  {:out-dir   "_site" ; where files get published; likely to be overridden
   :media-dir "media" ; org attachments to get copied into _site.
   :files-dir nil     ; where org content lives.
   :org-files nil     ; a list of org files, added to as files get converted.
   :curr-file curr-file})

(defn set-curr-file
  "Takes app-wide config and sets the current file being read on :curr-file"
  [config file]
  (let [cf (assoc curr-file :original file)
        config (assoc config :curr-file cf)]
    config))

(defn update-curr-file
  [config new-m]
  (let [new-curr-file (merge (config :curr-file) new-m)]
    (assoc config :curr-file new-curr-file)))

(defn default
  [files-dir]
  (merge starting-config
         {:out-dir       (str files-dir "_site/")
          :out-media-dir (str files-dir "_site/" (starting-config :media-dir))
          :files-dir     files-dir
          :media-dir     (str files-dir "/" (starting-config :media-dir))}))
