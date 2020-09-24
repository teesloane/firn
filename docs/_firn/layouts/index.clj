(defn header
  []
  [:header.index-nav.fade-in
   [:div.flex.align-start
    [:h1.h2.m0 "Firn"]
    [:a {:target "_blank" :href "https://github.com/theiceshelf/firn/releases"}
     [:sup.self-start "v.0.0.8"]]]
   [:div.h4.my2.italics "A static site generator for org-mode."]
   [:div.index-download-btns
    [:a.mx2 {:target "_blank" :href "https://github.com/theiceshelf/firn/releases"} "Download latest"]
    [:a.mx2 {:href "/getting-started"} "Read documentation"]]
   [:div {:style "position: absolute; bottom: 12vh;"}
    [:svg {:xmlns "http://www.w3.org/2000/svg", :fill "none", :viewbox "0 0 24 24", :height "24", :width "24"}
     [:path {:xmlns "http://www.w3.org/2000/svg", :fill-rule "evenodd", :clip-rule "evenodd", :d "M5.29289 6.29289C5.68342 5.90237 6.31658 5.90237 6.70711 6.29289L12 11.5858L17.2929 6.2929C17.6834 5.90237 18.3166 5.90237 18.7071 6.2929C19.0976 6.68342 19.0976 7.31658 18.7071 7.70711L12.7071 13.7071C12.3166 14.0976 11.6834 14.0976 11.2929 13.7071L5.29289 7.70711C4.90237 7.31658 4.90237 6.68342 5.29289 6.29289ZM5.29289 12.2929C5.68342 11.9024 6.31658 11.9024 6.70711 12.2929L12 17.5858L17.2929 12.2929C17.6834 11.9024 18.3166 11.9024 18.7071 12.2929C19.0976 12.6834 19.0976 13.3166 18.7071 13.7071L12.7071 19.7071C12.3166 20.0976 11.6834 20.0976 11.2929 19.7071L5.29289 13.7071C4.90237 13.3166 4.90237 12.6834 5.29289 12.2929Z", :fill "#afafaf"}]]]])

(defn index
  [{:keys [render build-url partials site-url] :as data}]
  (let [{:keys [head nav footer]} partials]
    [:html
     (head build-url)
     [:body
      [:div.fade-in (nav build-url)]
      (header)
      [:main.content {:style "max-width: 54em"}
       [:div.py3
        (render "Details" :content)]]

      (footer)]]))
