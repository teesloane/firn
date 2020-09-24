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
         (when-let [backlinks (render :backlinks)]
           [:div
            [:hr]
            [:div.backlinks
             [:h4 "Backlinks to this document:"]
             backlinks]])

         [:div.adjacent-files
          [:span (render :adjacent-files)]]
         (footer)]]
       (let [toc (render :toc)]
         (when (seq toc)
           [:aside#toc.def-toc.unfocused
            [:div [:b "Contents"] [:div (render :toc)]]]))]]]))
