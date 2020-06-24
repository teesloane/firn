(defn render-site-map
  [sm]
  (->> sm
    (sort-by :firn-order)
    (map #(vector :div.pb1 [:a {:href (% :path)} (% :title)]))))

(defn default
  [{:keys [site-map title render partials]}]
  (let [{:keys [head]} partials]
    (head
     [:body
      [:main
       [:article.def-wrapper
        [:aside.def-sidebar
         (render-site-map site-map)]
        [:div.def-content
         [:h1 title]
         [:div (render :toc)] ;; Optional; add a table of contents

         (render :file)]]]])))
