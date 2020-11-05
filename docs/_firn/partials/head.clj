(defn head
  [{:keys [build-url site-title]}]
  [:head
   [:meta {:charset "UTF-8"}]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
   [:link {:rel "stylesheet" :href "https://fonts.googleapis.com/css2?family=IBM+Plex+Sans:ital,wght@0,400;0,500;1,400&display=swap"}]
   [:link {:rel "stylesheet" :href (build-url "/static/css/prism.css")}]
   [:link {:rel "stylesheet" :href (build-url "/static/css/firn_base.css")}]
   [:link {:rel "stylesheet" :href (build-url "/static/css/basscss.css")}]
   [:link {:rel "stylesheet" :href (build-url "/static/css/customization.css")}]
   [:link {:rel "icon" :type "image/png" :href (build-url "/static/img/favico.png")}]
   [:title site-title]
   [:script {:src (build-url "/static/js/prism.js")}]])
