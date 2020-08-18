(defn default
  [{:keys [ build-url title render partials]}]
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

         [:h1 title]
         [:div (render :toc)] ;; Optional; add a table of contents
         (render :file)
         (when-let [backlinks (render :backlinks)]
           [:div
            [:hr]
            [:div.backlinks
             [:h4 "Backlinks to this document:"]
             backlinks]])

         [:div.adjacent-files
          [:span (render :adjacent-files)]]

         (footer)]]]]]))
