(defn project
  [{:keys [site-map render title partials file-logs]}]

  (let [{:keys [head nav]} partials]
    (head
     [:body.baskerville.flex.flex-column.lh-copy.h-100
      (nav)
      [:main.flex.flex-grow-1.h-100
       [:article.br.flex-auto.overflow-scroll.pa4.w-50
        [:h1 title]
        [:div (render "Notes" :content)]]
       [:aside.br.flex-auto.overflow-scroll.w-30
        ;; good place for a partial.
        (when-let [resources (render "Resources" :content)]
          [:details {:open "true"}
           [:summary.bb.pa3   "Resources"]
           [:div.bg-near-white.pa2.bb resources]])

        (when-not (empty? file-logs)
          [:details
           [:summary.bb.pa3  "Logbook"]
           [:div.bg-near-white.pa3.bb
            (for [x file-logs
                  :let [{:keys [year month day]} (x :start)]]
              [:div
               [:span year "/" month "/" day " - " (x :duration)]])]])

        (when-let [tasks-headline (render "Tasks" :title-raw)]
          [:details
           [:summary.bb.pa3 tasks-headline]
           [:div.bg-near-white.pa3.bb (render "Tasks" :content)]])]
       [:aside.br.flex-auto.overflow-scroll.pa4.w-20
        "Right bar"]]])))
