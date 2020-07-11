(defn render-site-map
  [sm]
  (->> sm
    (sort-by :firn-order)
    (map #(vector :div.pb1 [:a {:href (% :path)} (% :title)]))))

(defn tags
  [{:keys [site-map site-tags site-url partials]}]
  (let [{:keys [head nav footer]} partials]
    (head site-url
     [:body
      (nav)
      [:main
       [:article.def-wrapper
        [:aside#sidebar.def-sidebar
         (render-site-map site-map)]
        [:div.def-content
         [:h1 "Tags"]
         (for [[tag-name tags] site-tags]
           [:div
            [:h2 {:id tag-name :class "firn-tag-heading"} tag-name]
            (for [tag tags
                  :let [link (str site-url (tag :headline-link))]]
              [:div
               [:a
                {:href link}
                (tag :from-file) " - " (tag :from-headline)]])])
         (footer)]]]])))
