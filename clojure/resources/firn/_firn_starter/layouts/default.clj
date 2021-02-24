(defn default
  [{:keys [render partials] :as config}]
  (let [{:keys [head]} partials]
    [:html
     (head config)
     [:body
      [:main
       [:article.content.rss ;; `.rss` is required for targeting what content you wish your feed to contain
        [:div (render :file)]]]]]))
