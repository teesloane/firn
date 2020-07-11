(defn default
  [{:keys [render partials site-url]}]
  (let [{:keys [head]} partials]
    (head site-url
     [:body
      [:main
       [:article.content
        ;; [:div (render :toc)] ;; Optional; add a table of contents
        [:div (render :file)]]]])))
