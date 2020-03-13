;; (defn sidebar
;;   [config]
;;   [:aside.fl.w-25.pa2
;;    [:div.pa2.pr4 "Sidebar stuff"]])

(defn template
  "Renders a templates as if a project."
  [config wrapper]
  (wrapper
   [:main.debug.m7
    [:div.fl.w-10.pa2]
    [:article.fl.w-30.pa2
     [:div.pa2 "content"]]]))


(fn [config wrapper]
  (template config wrapper))
