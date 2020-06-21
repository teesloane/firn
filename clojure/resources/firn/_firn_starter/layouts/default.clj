(defn default
  [{:keys [render partials]}]
  (let [{:keys [head]} partials]
    (head
     [:body
      [:main
       [:article
        ;; [:div (render :toc)] ;; Optional; add a table of contents
        [:div (render :file)]]]])))
