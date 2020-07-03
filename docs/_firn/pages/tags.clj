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
  [{:keys [site-map site-links partials]}]
  (let [{:keys [head nav footer]} partials]
    (head
     [:body
      (nav)
      [:main
       [:article.def-wrapper
        [:aside#sidebar.def-sidebar
         (render-site-map site-map)]
        [:div.def-content
         [:div "This is a temporary page that will be updated in v0.0.7"]
         ; (for [x site-map] [:div (str x)])
         (footer)]]]])))
