(fn [body]
  [:html
   [:head
    [:link {:rel "stylesheet" :href "./assets/css/tachyons.css"}]
    [:link {:rel "stylesheet" :href "./assets/css/main.css"}]]
   [:body body]])
