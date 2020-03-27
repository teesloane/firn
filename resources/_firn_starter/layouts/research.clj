(defn research
  [{:keys [render title partials]}]
  (let [{:keys [head nav]} partials]
    (head
     [:body
      (nav)
      [:main
       [:article
        [:h1 title]
        [:div (render "Notes" :content)]]
       [:aside
        [:details {:open "true"}
         [:summary  "Resources"]
         [:div (render "Resources" :content)]]]]])))
