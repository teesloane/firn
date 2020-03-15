(defn layout-default
  "Renders a templates as if a project.
  Someday this will become a macro. Maybe."
  [config]
  (let [{:keys [ head ]} (config :partials)
        render                (config :render)
        content               (-> config :curr-file :as-edn)]

    (head
     [:body
      [:main
       [:article
        [:div (render content)]]
       [:aside
        [:div ""]
        [:div ""]]]])))
