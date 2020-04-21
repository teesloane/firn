(defn head
  [body]
  [:html
   [:head
    [:link {:rel "stylesheet" :href "/static/css/bass.css"}]
    [:link {:rel "stylesheet" :href "/static/css/main.css"}]]
   body])
