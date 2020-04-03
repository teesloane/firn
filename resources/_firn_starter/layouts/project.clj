(defn project
  [{:keys [site-map render title partials file-logs]}]
  (prn "file logs are " file-logs)
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
