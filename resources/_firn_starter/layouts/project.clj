;; (defn project
;;   "Renders a templates as if a project.
;;   Someday this will become a macro. Maybe."
;;   [config]
;;   (let [{:keys [head nav]} (config :partials)
;;         render                (config :render)
;;         content               (-> config :curr-file :as-edn)
;;         get-headline          (-> config :get-headline)
;;         get-headline-content  (-> config :get-headline-content)
;;         file-title            (-> config :curr-file :org-title)

;;         notes                 (get-headline-content content "Notes")
;;         meta                  (get-headline content "Meta")
;;         resources             (get-headline-content content "Resources")
;;         tasks                 (get-headline-content content "Tasks")
;;         task-headline         (-> (get-headline content "Tasks") :children first :raw)]


;;     (head
;;      [:body
;;       (nav)
;;       [:main
;;        [:article
;;         [:h1 file-title]
;;         [:div (render notes)]]
;;        [:aside
;;         [:details {:open "true"}
;;          [:summary  "Resources"]
;;          [:div (render resources)]]
;;         [:details
;;          [:summary task-headline]
;;          [:div (render tasks)]]]]])))


(defn project
  "Renders a templates as if a project.
  Someday this will become a macro. Maybe."
  [{:keys [render] :as config}]
  (let [{:keys [head nav]} (config :partials)
        ;; render                (config :render)
        content               (-> config :curr-file :as-edn)
        get-headline          (-> config :get-headline)
        get-headline-content  (-> config :get-headline-content)
        file-title            (-> config :curr-file :org-title)

        notes                 (get-headline-content content "Notes")
        meta                  (get-headline content "Meta")
        resources             (get-headline-content content "Resources")
        tasks                 (get-headline-content content "Tasks")
        task-headline         (-> (get-headline content "Tasks") :children first :raw)]


    (head
     [:body (render "Notes")])))
