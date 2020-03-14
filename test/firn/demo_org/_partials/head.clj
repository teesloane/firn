(fn [body]
  [:html
   [:head
    [:link {:rel "stylesheet" :href "./assets/css/main.css"}]
    [:link {:rel "stylesheet" :href "./assets/css/tachyons.css"}]]
   [:body body]])
