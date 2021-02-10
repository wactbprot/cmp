(ns cmp.ui.listener
  (:require
     [hiccup.form :as hf]
     [hiccup.page :as hp]
     [cmp.ui.core :as ui]))

(defn table
  [conf data]
  (if (empty? data)
    [:span {:class "tag is-info"} "no listeners"]
    (into [:table {:class "table"}
           [:thead
            [:tr [:th "mp-id"] [:th "struct"] [:th "no-idx"] [:th "func"] [:th "level"] [:th "listener-id"]]]]
          (mapv 
           (fn [{:keys [mp-id struct no-idx func level listener-id]}]
             [:tr
              [:td mp-id]
              [:td struct]
              [:td no-idx]
              [:td func]
              [:td level]
              [:td listener-id]]) data))))
  
(defn view [conf data] (ui/index conf (table conf data)))
