(defn template
  "Renders a templates as if a project."
  [config]
  (let [{:keys [ head nav ] } (config :partials)]
    (head
     [:main.debug.m7
      (nav)
      [:div.fl.w-10.pa2]
      [:article.fl.w-30.pa2
       [:div.pa2 "foo"]]])))
