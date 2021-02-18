(ns cmp.ui.mp-meta
  (:require
   [cmp.ui.core   :as ui]
   [cmp.api-utils :as au]))

(defn card-content
  [conf m]
  [:div {:class "card-content"}  
   [:div {:class "columns"}
    [:div {:class "column"}
     [:figure {:class "image is-3by1"}
      (ui/img conf m "../../")]]
    [:div {:class "column"}
     [:p [:b "Measurement Prog.: " (:mp-id m)]]
     [:p [:b "Standard: "          (:std m)]]
     [:p [:b "Description: "] [:i (:descr m)]]]]
   [:p [:i "No of definitions: " [:span {:class "tag"} (:ndefins m)]]]
   [:p [:i "No of containers: "  [:span {:class "tag"}  (:ncont m)]]]
   (when-not (empty? (:docs m))
     [:p
      (into [:b "Documents: "]
            (mapv (fn [d] [:i (:doc-id d) "/" (:doc-version d)] ) (:docs m)))])])

(defn card
  [conf m]
  (ui/card-template conf m (card-content conf m) (ui/card-footer conf m)))

(defn view [conf req data] (ui/index conf req (card conf data)))
