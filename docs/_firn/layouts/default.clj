(defn default
  [{:keys [org-tags date-updated build-url site-title title render partials] :as data}]
  (let [{:keys [head nav footer]} partials]
    [:html
     (head data)
     [:body
      (nav build-url)
      [:main.def-wrapper
       [:aside#sidebar.def-sidebar.unfocused
        (render :sitemap {:sort-by :firn-order})]
       [:article.def-content-wrap
        [:div.def-content
         [:h1.mb0 title]
         (when date-updated
           [:div.flex.h6.mb1
            [:div.pr1 "Last updated: "]
            [:div.italic date-updated]])
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
