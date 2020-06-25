(defn head
  [body]
  [:html
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
    [:link {:rel "stylesheet" :href "/static/css/hljs-tomorrow.css"}]
    [:link {:rel "stylesheet" :href "/static/css/firn_base.css"}]
    [:link {:rel "stylesheet" :href "/static/css/basscss.css"}]
    [:link {:rel "stylesheet" :href "/static/css/customization.css"}]
    [:link {:rel "icon" :type "image/png" :href "/data/favico.png"}]
    [:script {:src "/static/js/highlight.pack.js"}]
    [:script "hljs.initHighlightingOnLoad()"]]
   body])
