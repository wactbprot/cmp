(ns cmp.ui.listener
  (:require
     [cmp.ui.core :as ui]))


(defn card
  [conf data]
  [:div {:class "card"}
   [:div {:class "card-image"}
    [:figure {:class "image is-3by1"}
     [:img {:src "../img/redis-logo.png" :alt "Placeholder image"}]]
   
   [:div {:class "card-content"}
    [:div {:class "media-content"}
     [:p {:class "title is-8"} "Listener"]]
   
    [:div {:class "content"}
     [:p {:class " is-8"}  [:b "Measurement Prog.: "] (:mp-id data)]
     [:p {:class " is-8"}  [:b "Level: "] (:level data)]
     [:p {:class " is-8"}  [:b "Pattern: "] (:key data)]
     [:p {:class " is-8"}  [:b "Id: "] (:listener-id data)]]]]])

(defn view [conf data]
  (prn data)
  (ui/index conf (into [:div] (map (fn [l] (card conf l)) data))))
