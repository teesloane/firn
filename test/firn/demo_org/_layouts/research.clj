(defn layout-project
  "Renders a templates as if a project.
  Someday this will become a macro. Maybe."
  [config]
  (let [{:keys [head nav]} (config :partials)
        render                (config :render)
        content               (-> config :curr-file :as-edn)
        get-headline          (-> config :get-headline)
        notes                 (-> (get-headline content "Notes") :children second)
        meta                  (get-headline content "Meta")
        resources             (get-headline content "Resources")]

    (head
     [:body
      (nav)
      [:main
       [:article
        [:div (render notes)]]
       [:aside
        [:div (render resources)]]]])))
