(defn mobile-btn
  []
  [:div#nav-icon {:onClick "toggleMenu()"}
   [:span] [:span] [:span] [:span]])

(defn nav
  [site-url]
  (let [links [["/"         "Home"]
               ["/getting-started"    "Docs"]
               ["https://github.com/theiceshelf/firn"    "Github"]]]
    ;; left nav.
    [:nav.nav
     [:div.nav-container
      [:div.nav-left
       (mobile-btn)
       [:img.nav-logo {:width 32 :src (str site-url "/static/img/ico-light.png")}]]
      [:div.nav-links
       (for [l links]
         [:a.nav-links-item {:href (first l)}
          [:span (second l)]])]]]))
