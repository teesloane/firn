(defn head
  [body]
  [:html
   [:head
    [:link {:rel "stylesheet" :href "/assets/css/main.css"}]]
   body])
