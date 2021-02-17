(ns cmp.ui.mp-meta
  (:require
     [cmp.ui.core :as ui]))

(defn card-content
  [conf m]
  [:div {:class "card-content"}
   [:div {:class "content"}
    [:p {:class "is-8"} [:b "Measurement Prog.: "] (:mp-id m)]
    [:p {:class "is-8"} [:b "Standard: "]          (:std m)]
    [:p {:class "is-8"} [:b "Description: "]       (:descr m)]
    [:p {:class "is-8"} [:b "No of definitions: " [:span {:class "tag"} (:ndefins m)]]]
    [:p {:class "is-8"} [:b "No of containers: " [:span {:class "tag"}  (:ncont m)]]]
    (when-not (empty? (:docs m))
      [:p {:class "is-8"}
       (into [:b "Documents: "]
             (mapv (fn [d] [:i (:doc-id d) "/" (:doc-version d)] ) (:docs m)))])]])

(defn card-footer
  [conf m]
  [:footer {:class "card-footer"}
   [:span {:class "card-footer-item"}
    (ui/ctrl-link (assoc m :struct "container"))]])

(defn card
  [conf m]
  [:div {:class "content"}
   [:div {:class "card"}
    [:div {:class "card-image"}
     [:figure {:class "image is-3by1"}
      (ui/img conf m "../../")]
     (card-content conf m)
     (card-footer conf m)]]])

(defn view
  [conf data mp]
  (ui/index conf (card conf data) mp))
