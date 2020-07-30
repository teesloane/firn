(defn tags
  [{:keys [site-tags build-url site-url partials]}]
  (let [{:keys [head]} partials]
    [:html
     (head build-url)
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
               [:a {:href (str site-url (tag :headline-link))}
                (tag :from-file) " - "
                (tag :from-headline)]])])]]]]]))
