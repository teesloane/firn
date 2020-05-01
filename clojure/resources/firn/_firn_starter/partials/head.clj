(defn head
  [body]
  [:html
   [:head
    [:link {:rel "stylesheet" :href "/static/css/bass.css"}]
    [:meta {:charsset "UTF-8"}]
;; <meta http-equiv="Content-type" value="text/html; charset=UTF-8" />
    [:link {:rel "stylesheet" :href "/static/css/main.css"}]]
   body])
