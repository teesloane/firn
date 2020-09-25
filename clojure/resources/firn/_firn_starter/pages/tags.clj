(defn tags
  [{:keys [build-url render partials]}]
  (let [{:keys [head]} partials]
    [:html
     (head build-url)
     [:body
      [:main
       [:article.def-wrapper
        [:div.content
         [:h1 "Org Tags"]
         (render :org-tags)
         [:hr]
         [:h1 "File Tags"]
         (render :firn-tags)]]]]]))
