(defn template
  "Renders a templates as if a project."
  [config]
  (let [{:keys [ head nav ] } (config :partials)
        render                (config :render)
        content               (-> config :curr-file :as-edn)
        ;; _ (prn "RENDER IS " content)
        get-headline          (-> config :get-headline)
        meta                  (get-headline content "Notes")
        _                     (prn "META IS " meta)]

    (head
     [:main.m7
      (nav)
      [:article.fl.w-30.pa2
       [:div.pa2 "yo"]]
      [:article.fl.w-70.pa2
       [:div.pa2 (render meta)]]])))
