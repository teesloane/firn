(defn default
  [{:keys [title render partials]}]
  (let [{:keys [head nav footer]} partials]
    (head
     [:body
      (nav)
      [:main
       [:article.def-wrapper
        [:div.def-content
         [:h1 title]
         ;; [:div (render :toc)] ;; Optional; add a table of contents
         (render "Example content")
         [:hr]
         [:h2 "Render function examples"]
         [:div "The following examples demonstrate the ability of the 'render' function; each code sample demonstrates usage, as well as the output below it."]
         [:div
          [:h3 "Rendering a specific heading"]
          [:pre [:code "(render \"Find and render me!\")"]]
          (render "Find and render me!")
          [:hr]]
         [:div
          [:h3 "Rendering a table of contents"]
          [:pre [:code "(render :toc)"]]
          (render :toc)
          [:hr]]
         [:div
          [:h3 "Rendering a table of contents, staring at a specific headline."]
          [:pre [:code "(render :toc {:headline \"Headings\"})"]]
          (render :toc {:headline "Headings"})
          [:hr]]
         [:div
          [:h3 "Rendering a table of contents, up to a specific depth."]
          [:pre [:code "(render :toc {:depth 1})"]]
          (render :toc {:depth 1})
          [:hr]]
         [:div
          [:h3 "Rendering a table of contents, up to a specific depth, starting at a specific heading, but excluding the heading started at."]
          [:pre [:code "(render :toc {:headline \"Headings\" :depth 1 :exclusive? true})"]]
          (render :toc {:headline "Headings" :depth 1 :exclusive? true})
          [:hr]]
         [:div
          [:h3 "Rendering the sum Logbook of this file, as a polyline svg. This is a rather opinionated 'render'; it sorts your logbooks into years and the renders a graph for each year. "]
          [:pre [:code "(render :logbook-polyline {:height 70 :stroke \"salmon\"})"]]
          (render :logbook-polyline {:height 70 :stroke "salmon"})
          [:hr]]]]]
      (footer)])))
