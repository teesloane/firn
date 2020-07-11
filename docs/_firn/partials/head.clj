(defn head
  [site-url body]
  (let [resource #(str site-url "/static/" %)]
    [:html
     [:head
      [:meta {:charset "UTF-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
      [:link {:rel "stylesheet" :href (resource "css/hljs-tomorrow.css")}]
      [:link {:rel "stylesheet" :href (resource "css/firn_base.css")}]
      [:link {:rel "stylesheet" :href (resource "css/basscss.css")}]
      [:link {:rel "stylesheet" :href (resource "css/customization.css")}]
      [:link {:rel "icon" :type "image/png" :href "/data/favico.png"}]
      [:script {:src (resource "js/highlight.pack.js")}]
      [:script "hljs.initHighlightingOnLoad()"]]
     body]))
