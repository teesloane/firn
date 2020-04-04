(defn project
  [{:keys [site-map render title partials file-logs]}]

  (let [{:keys [head nav]} partials]
    (head
     [:body.baskerville.flex.flex-column.lh-copy
      (nav)
      [:main.flex.flex-grow-1
       [:article.br.overflow-scroll.pa4.w-50.vh-100
        [:h1 title]
        [:div (render "Notes" :content)]]
       [:aside.br.pa4.w-30.overflow-scroll.vh-100

        (when-let [resources (render "Resources" :content)]
          [:details {:open "true"}
           [:summary  "Resources"]
           [:div resources]])

        (when-not (empty? file-logs)
          [:details
           [:summary  "Logbook"]
           [:div (for [x file-logs
                       :let [{:keys [year month day]} (x :start)]]
                   [:div
                    [:span year "/" month "/" day " - " (x :duration)]])]])

        (when-let [tasks-headline (render "Tasks" :title-raw)]
          [:details
           [:summary tasks-headline]
           [:div (render "Tasks" :content)]])]]])))
