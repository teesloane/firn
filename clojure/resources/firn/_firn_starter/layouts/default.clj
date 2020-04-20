(defn default
  [{:keys [ yield partials]}]
  (let [{:keys [head]} partials
        default-styles {:style "max-width: 700px; padding: 32px; margin: 0 auto"}]
    (head
     [:body default-styles
      [:main
       [:article
        [:div yield]]
       [:aside
        [:div ""]
        [:div ""]]]])))
