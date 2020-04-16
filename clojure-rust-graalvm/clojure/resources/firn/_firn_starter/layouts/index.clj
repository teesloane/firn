(defn index
  [{:keys [site-map partials site-logs]}]
  (let [{:keys [head]} partials
        projects       (filter #(= (% :firn-under) "project") site-map)
        research       (filter #(= (% :firn-under) "research") site-map)]
    (head
     [:body.page-index
      [:main.flex.pa4.sans-serif
       [:section.flex.flex-column.center
        [:h3 "Projects and Experiments"]
        ;; Projects
        [:article.mb4
         (for [l projects]
           [:div.fl.w-third.pa0.mv2
            [:a.f6 {:href (l :path)} (l :title)]])]
        ;; Research
        [:h3 "Research"]
        [:article
         (for [l research]
           [:div.fl.w-third.pa0.mv2
            [:a.f6 {:href (l :path)} (l :title)]])]]]])))
