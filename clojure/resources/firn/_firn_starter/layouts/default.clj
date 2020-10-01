(defn default
  [{:keys [render partials build-url site-title site-author site-desc] :as config}]
  (let [{:keys [head]} partials]
    [:html
     (head config)
     [:body
      [:main
       [:article.content
        ;; [:div (render :toc)] ;; Optional; add a table of contents
        [:div (render :file)]]]]]))
