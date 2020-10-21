(defn tags
  [{:keys [render partials build-url site-title site-author site-desc] :as config}]
  (let [{:keys [head]} partials]
    [:html
     (head config)
     [:body
      [:main
       [:article.def-wrapper
        [:div.content
         [:h1 "Org Tags"]
         (render :org-tags)
         [:hr]
         [:h1 "File Tags"]
         (render :firn-tags)]]]]]))
