(defn tags
  [{:keys [build-url render partials]}]
  (let [{:keys [head nav footer]} partials]
    [:html
     (head build-url)
     [:body
      (nav build-url)
      [:main
       [:article.def-wrapper
        [:aside#sidebar.def-sidebar
         (render :sitemap {:sort-by :firn-order})]
        [:div.def-content
         [:h1 "Org Tags"]
         (render :org-tags)
         [:hr]
         [:h1 "File Tags"]
         (render :firn-tags)
         (footer)]]]]]))
