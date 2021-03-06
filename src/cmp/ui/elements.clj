(ns cmp.ui.elements
  (:require
   [cmp.config        :as config]
   [cheshire.core     :as che]
   [cmp.ui.index      :as ui]
   [cmp.handler-utils :as au]
   [cmp.st-utils      :as stu]
   [cmp.utils         :as u]))


(defn post-url [m] (str (:mp-id m) "/exchange"))

(defn label [conf k s]
  [:div {:class "field-label"}
   [:label {:class "label"} s]])

(defn input [conf k g kw m]
  [:div {:class "field-body"}
    [:div {:class "field"}
     [:p {:class "control"}
      [:input {:class "input is-info value"
               :value (kw m)
               :data-url (post-url g)
               :data-key  (str k "." (name kw))}]]]])

(defn ready-button [conf k g m]
  [:button {:class "button is-info setter"
            :data-value (che/encode {:Ready true})
            :data-url (post-url g)
            :data-key  k} "ok"])

(defn select [conf k g m]
  [:div {:class "select"}
   (into [:select] 
         (mapv (fn [e] [:option {:value (:value e)} (:display e)])
               (:Select m)))])

(defn type-unit-value [conf k g m]
  [:div {:class "field is-horizontal"}
   (label conf k "Type") (input conf k g :Type m)
   (label conf k "Value")(input conf k g :Value m)
   (label conf k "Unit") (input conf k g :Unit m)])

(defn caption-type-unit-value [conf k g m]
  [:div {:class "field is-horizontal"}
   (label conf k "Type") (input conf k g :Type m)
   (label conf k "Value")(input conf k g :Value m)
   (label conf k "Unit") (input conf k g :Unit m)])

(defn selected-ready [conf k g m]
  [:div {:class "field is-horizontal"}
   (select conf k g m)
   (ready-button conf k g m)])

(defn elem [conf k g m]
  (let [ks (keys m)]
    (cond 
      (every? #{:Caption :Type :Unit :Value} ks) (caption-type-unit-value  conf k g m)
      (every? #{:Type :Unit :Value} ks) (type-unit-value  conf k g m)
      (every? #{:Selected :Select :Ready} ks)    (selected-ready   conf k g m)
      :default [:div k m])))

(defn content [conf req data]
  (into [:section]
        (mapv
         (fn [m]
           (when-not (empty? (:elem-values m))
             [:div {:class "box"}
              [:h1 (:title m)]
              (into [:p]
                    (mapv (fn [k v]
                            (elem conf k m v))
                          (:elem-keys-vec m)
                          (:elem-values m)))]))
         data)))

(defn view [conf req data] (ui/index conf req (content conf req data)))
