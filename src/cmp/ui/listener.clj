(ns cmp.ui.listener
  (:require
     [hiccup.form :as hf]
     [hiccup.page :as hp]
     [cmp.ui.core :as ui]))

(defn table
  [conf data]
    (prn data)
  (if (empty? data)
    [:table "no listeners"]
    [:table (into [] (flatten
             (mapv 
              (fn [{:keys [mp-id struct no-idx func level listener-id]}]
                [:tr
                 [:th mp-id]
                 [:th struct]
                 [:th no-idx]
                 [:th func]
                 [:th level]
                 [:th listener-id]]) data)))]))

(defn view [conf data] (ui/index conf (table conf data)))
