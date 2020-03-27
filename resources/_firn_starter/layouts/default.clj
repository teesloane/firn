(defn default
  [{:keys [ yield partials]}]
  (let [{:keys [head]} partials]
    (head
     [:body
      [:main
       [:article
        [:div yield]]
       [:aside
        [:div ""]
        [:div ""]]]])))
