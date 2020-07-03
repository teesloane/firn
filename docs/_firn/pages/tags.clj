;; (defn tags
;;   [{:keys [site-tags partials]}]
;;   [:div
;;    [:h1 "Tags Page"]])

(defn render-site-map
  [sm]
  (->> sm
    (sort-by :firn-order)
    (map #(vector :div.pb1 [:a {:href (% :path)} (% :title)]))))

(defn tags
  [{:keys [site-map partials]}]
  (let [{:keys [head nav footer]} partials]
    (head
     [:body
      (nav)
      [:main
       [:article.def-wrapper
        [:aside#sidebar.def-sidebar
         (render-site-map site-map)]
        [:div.def-content
         (footer)]]]])))
