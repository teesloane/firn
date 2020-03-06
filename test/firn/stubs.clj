(ns firn.stubs
  (:require  [clojure.test :as t]
             [cheshire.core :as json]))

(def simple-file
  {:type "document",
   :children
   [{:type "headline",
     :level 1,
     :children
     [{:type "title",
       :level 1,
       :raw "A simple file",
       :children
       [{:type "text", :value "A simple file"}]}
      {:type "section",
       :children
       [{:type "paragraph",
         :children
         [{:type "text",
           :value "Hi there"}]}]}]}]})


(def list-items
  {:type "list",
   :indent 0,
   :ordered true,
   :children
   [{:type "list-item",
     :bullet "1) ",
     :children
     [{:type "paragraph",
       :children
       [{:type "text", :value "hi\r"}]}]}
    {:type "list-item",
     :bullet "2) ",
     :children
     [{:type "paragraph",
       :children
       [{:type "text",
         :value "hithere"}]}]}]})

(-> list-items
    (firn.markup/to-html))
