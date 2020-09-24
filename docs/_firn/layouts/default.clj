(defn default
  [{:keys [org-tags build-url title render partials]}]
  (let [{:keys [head nav footer]} partials]
    [:html
     (head build-url)
     [:body
      (nav build-url)
      [:main.def-wrapper
       [:aside#sidebar.def-sidebar.unfocused
        (render :sitemap {:sort-by :firn-order})]
       [:article.def-content-wrap
        [:div.def-content
         [:h1 title]
         (render :file)

         [:div.adjacent-files
          [:span (render :adjacent-files)]]
         (footer)]]

       (let [toc       (render :toc)
             backlinks (render :backlinks)]
         [:aside#toc.def-toc.unfocused
          (when toc
            [:div
             [:h4 "Contents"]
             [:div (render :toc)]])
          (when backlinks
            [:div
             [:h4 "Backlinks"] backlinks])])]]]))
