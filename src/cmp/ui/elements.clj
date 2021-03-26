(ns cmp.ui.elements
  (:require
   [cmp.config        :as config]
   [cmp.ui.index      :as ui]
   [cmp.handler-utils :as au]
   [cmp.utils         :as u]))

(defn label
  [conf k s]
  [:div {:class "field-label"}
   [:label {:class "label"} s]])

(defn input
  [conf k x]
  [:div {:class "field-body"}
    [:div {:class "field"}
     [:p {:class "control"}
      [:input {:class "input is-info exchange"  :value x}]]]])

(defn type-unit-value
[conf k m]
  [:div {:class "field is-horizontal"}
   (label conf k "Type") (input conf k (:Type m))
   (label conf k "Value")(input conf k (:Value m))
   (label conf k "Unit") (input conf k (:Unit m))])

(defn selected
  [conf k m]
  [:div {:class "field is-horizontal"}
   [:div {:class "select exchange"}
    (into [:select] 
          (mapv (fn [e] [:option {:value (:value e)} (:display e)]) (:Select m)))]
   [:button {:class "button is-info exchange"} "ok"]])


(defn elem
  [conf k m]
  (let [ks (keys m)]
    (cond 
      (every? #{:Type :Unit :Value} ks)       (type-unit-value  conf k m)
      (every? #{:Selected :Select :Ready} ks) (selected  conf k m)
      :default [:div k m])))

(defn content
  [conf req data]
  (into [:section]
        (mapv
         (fn [m]
           (when-not (empty? (:elem-values m))
             [:div {:class "box"}
              [:h1 (:title m)]
              (into [:p]
                    (mapv (fn [k v]
                            (elem conf k v))
                          (:elem-keys-vec m)
                          (:elem-values m)))]))
         data)))

(defn view [conf req data] (ui/index conf req (content conf req data)))
