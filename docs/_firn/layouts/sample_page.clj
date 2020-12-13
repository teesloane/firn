(defn default
  [{:keys [title render build-url site-title partials site-url] :as data}]
  (let [{:keys [head nav footer]} partials]
    [:html
     (head data)
     [:body
      (nav build-url)
      [:main
       [:article.def-wrapper
        [:aside#sidebar.def-sidebar
         (render :sitemap {:sort-by :firn-order})]
        [:div.def-content
         [:h1 title]
         ;; [:div (render :toc)] ;; Optional; add a table of contents
         (render "Example content")
         [:hr]
         [:h2 "Render function examples"]
         [:div "The following examples demonstrate the ability of the 'render' function; each code sample demonstrates usage, as well as the output below it."]
         [:div
          [:h3 "Rendering a specific heading"]
          [:pre.language.language-clojure [:code "(render \"Find and render me!\")"]]
          (render "Find and render me!")
          [:hr]]
         [:div
          [:h3 "Rendering a table of contents"]
          [:pre.language.language-clojure [:code "(render :toc)"]]
          (render :toc)
          [:hr]]
         [:div
          [:h3 "Rendering a table of contents, staring at a specific headline."]
          [:pre.language.language-clojure [:code "(render :toc {:headline \"Headings\"})"]]
          (render :toc {:headline "Headings"})
          [:hr]]
         [:div
          [:h3 "Rendering a table of contents, up to a specific depth."]
          [:pre.language.language-clojure [:code "(render :toc {:depth 1})"]]
          (render :toc {:depth 1})
          [:hr]]
         [:div
          [:h3 "Rendering a table of contents, up to a specific depth, starting at a specific heading, but excluding the heading started at."]
          [:pre.language.language-clojure [:code "(render :toc {:headline \"Headings\" :depth 1 :exclude-headline? true})"]]
          (render :toc {:headline "Headings" :depth 1 :exclude-headline? true})
          [:hr]]
         [:div
          [:h3 "Rendering the sum Logbook of this file, as a polyline svg. This is a rather opinionated 'render'; it sorts your logbooks into years and the renders a graph for each year. "]
          [:pre.language.language-clojure [:code "(render :logbook-polyline {:height 70 :stroke \"salmon\"})"]]
          (render :logbook-polyline {:height 70 :stroke "salmon"})
          [:hr]]]]]
      (footer)]]))
