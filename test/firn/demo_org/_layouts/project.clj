(defn project
  "Renders a templates as if a project.
  Someday this will become a macro. Maybe."
  [config]
  (let [{:keys [head nav]} (config :partials)
        render                (config :render)
        content               (-> config :curr-file :as-edn)
        get-headline          (-> config :get-headline)
        get-headline-content  (-> config :get-headline-content)
        file-title            (-> config :curr-file :org-title)

        notes                 (-> (get-headline-content content "Notes"))
        meta                  (get-headline content "Meta")
        resources             (get-headline content "Resources")
        tasks                 (get-headline content "Tasks")]

    (head
     [:body
      (nav)
      [:main
       [:article
        [:h1 file-title]
        [:div (render notes)]]
       [:aside
        [:div (render resources)]
        [:div (render tasks)]]]])))
