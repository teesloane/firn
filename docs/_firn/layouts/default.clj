(defn render-site-map
  [sm]
  (->> sm
     (sort-by :firn-order)
     (map #(vector :div.pb1 [:a {:href (% :path)} (% :title)]))))

(defn default
  [{:keys [site-map site-url title render partials]}]
  (let [{:keys [head nav footer]} partials]
    (head site-url
          [:body
           (nav site-url)
           [:main
            [:article.def-wrapper
             [:aside#sidebar.def-sidebar
              (render-site-map site-map)]
             [:div.def-content
              [:h1 title]
              [:div (render :toc)] ;; Optional; add a table of contents
              (render :file )
              (footer)]]]])))
