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
     [:p {:class "title is-4"} "Listener"]
     [:p {:class "subtitle is-8"}  [:b "Measurement Prog.: "] (:mp-id data)]
     [:p {:class "subtitle is-8"}  [:b "Level: "] (:level data)]
     [:p {:class "subtitle is-8"}  [:b "Pattern: "] (:key data)]
     [:p {:class "subtitle is-8"}  [:b "Id: "] (:listener-id data)]]]
   
   [:div {:class "content"}
    [:p (str "reigistered listener for measurement definition " (:mp-id data) "is running") ]]]])

(defn view [conf data]
  (prn data)
  (ui/index conf (into [:div] (map (fn [l] (card conf l)) data))))
