(defn tags
  [{:keys [build-url org-tags partials]}]
  (let [{:keys [head nav footer]} partials]
    [:html
     (head build-url)
     [:body
      (nav build-url)
      [:main
       [:article.def-wrapper
        [:aside#sidebar.def-sidebar]

        [:div.def-content
         [:h1 "Tags"]
         (for [[tag-name tags] org-tags]
           [:div
            [:h2 {:id tag-name :class "firn-tag-heading"} tag-name]
            (for [tag tags
                  :let [link (tag :headline-link)]]
              [:div
               [:a
                {:href link}
                (tag :from-file) " - " (tag :from-headline)]])])
         (footer)]]]]]))
