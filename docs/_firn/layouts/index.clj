(defn header
  []
  [:header.index-nav
   [:div.flex.align-start
    [:h1.h2.m0 "Firn"]
    [:a {:target "_blank" :href "https://github.com/theiceshelf/firn/releases"}
     [:sup.self-start "v.0.0.5"]]]
   [:div.h4.my2.italics "A static site generator for org-mode."]
   [:div.index-download-btns
    [:a.mx2 {:target "_blank" :href "https://github.com/theiceshelf/firn/releases"} "Download latest"]
    [:a.mx2 {:href "/getting-started"} "Read documentation"]]])

(defn index
  [{:keys [render partials] :as data}]
  (let [{:keys [head nav footer]} partials]
    (head
     [:body
      (nav)
      (header)
      [:main.content
       [:div.py3
        (render "Details" :content)]]
      (footer)])))
