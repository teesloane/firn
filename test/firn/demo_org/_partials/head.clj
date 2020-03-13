(fn [body]
  [:html
   [:head
    [:link {:rel "stylesheet" :href "https://unpkg.com/tachyons@4.10.0/css/tachyons.min.css"}]
    [:link {:rel "stylesheet" :href "./assets/css/main.css"}]]
   [:body body]])
