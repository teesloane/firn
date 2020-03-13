(defn nav
  [& args]
  [:nav.w-100.pa2
   [:h1 "Doc Name"]
   [:h4 "Sub info"]
   [:ul
    [:li [:a {:href "#"} "Link 1"]]
    [:li [:a {:href "#"} "Link 2"]]
    [:li [:a {:href "#"} "Link 3"]]]])
