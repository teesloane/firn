(defn project
  [{:keys [site-map render title partials file-logs]}]
  (let [{:keys [head nav]} partials]
    (head
     [:body.baskerville.lh-copy
      (nav)
      [:main.flex.min-h-100
       [:article.br.pa4.w-40
        [:h1 title]
        [:div (render "Notes" :content)]]
       [:aside.br.pa4.w-20
        [:details {:open "true"}
         [:summary  "Resources"]
         [:div (render "Resources" :content)]]
        [:details {:open "true"}
         [:summary  "Logbook"]
         [:div (for [x file-logs
                     :let [{:keys [year month day]} (x :start)]]
                 [:div
                  [:span year "/" month "/" day " - " (x :duration)]])]]
        [:details
         [:summary (render "Tasks" :title-raw)]
         [:div (render "Tasks" :content)]]]]])))
