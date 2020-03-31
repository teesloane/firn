(defn index
  [{:keys [site-map partials]}]
  (let [{:keys [head]} partials]
    (head
     [:body.page-index
      [:main
       [:article (for [l site-map]
                   [:div
                    [:a {:href (l :path)} (l :title)]])]]])))
