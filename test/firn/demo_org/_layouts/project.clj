(defn template
  "Renders a templates as if a project.
  Someday this will become a macro. Maybe."
  [config]
  (let [{:keys [ head nav ] } (config :partials)
        render                (config :render)
        content               (-> config :curr-file :as-edn)
        get-headline          (-> config :get-headline)
        notes                 (get-headline content "Notes")
        meta                  (get-headline content "Meta")
        tasks                 (get-headline content "Tasks")]

    (head
     [:main.wiki
      (nav)
      [:article.fl.w-30.pv2.pr4.sidebar
       [:div.pa2 (render tasks)]]
      [:article.fl.w-70.pa2
       [:div.pa2 (render notes)]]])))
