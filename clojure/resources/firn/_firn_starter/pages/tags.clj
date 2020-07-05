(defn tags
  [{:keys [site-tags partials]}]
  (let [{:keys [head]} partials]
    (head
     [:body
      [:main
       [:article
        [:div.content
         [:h1 "Tags"]
         (for [[tag-name tags] site-tags]
           [:div
            [:h2 {:id tag-name :class "firn-tag-heading"} tag-name]
            (for [tag tags]
              [:div
               [:a {:href (tag :headline-link)}
                (tag :from-file) " - "
                (tag :from-headline)]])])]]]])))
