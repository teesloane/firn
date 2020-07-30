(defn head
  [build-url]
  [:head
   [:meta {:charset "UTF-8"}]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
   [:link {:rel "stylesheet" :href (build-url "/static/css/hljs-tomorrow.css")}]
   [:link {:rel "stylesheet" :href (build-url "/static/css/firn_base.css")}]
   [:link {:rel "stylesheet" :href (build-url "/static/css/basscss.css")}]
   [:link {:rel "stylesheet" :href (build-url "/static/css/customization.css")}]
   [:link {:rel "icon" :type "image/png" :href (build-url "/static/img/favico.png")}]
   [:script {:src (build-url "/static/js/highlight.pack.js")}]
   [:script "hljs.initHighlightingOnLoad()"]])
