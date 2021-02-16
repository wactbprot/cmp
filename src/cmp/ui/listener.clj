(ns cmp.ui.listener
  (:require
     [cmp.ui.core :as ui]))

(defn img [conf data] [:img {:src (str "../img/" (get-in conf [:ui :img (keyword (:mp-id data))] "default.jpg"))}])

(defn label
  [conf data]
  (let [cl (condp = (keyword (:level data))
            :a "tag is-primary"
            :b "tag is-info"
            :c "tag is-info is-light")
        cf (condp = (keyword (:func data))
             :ctrl "tag is-dark"
             :state "tag is-light"
             "tag is-light")]
    [:div {:class "control"}
     [:div {:class "tags has-addons"}
      [:span {:class cf} "funtion: " (:func data)]
      [:span {:class cl} "level: "  (:level data)]]]))

(defn card
  [conf data]
  [:div {:class "content"}
  [:div {:class "card"}
   [:div {:class "card-image"}
    [:figure {:class "image is-3by1"}
     (img conf data)]
   
   [:div {:class "card-content"}
    [:div {:class "content"}
     [:p {:class " is-8"}  [:b "Measurement Prog.: "] (:mp-id data)]
     (label conf data)]]]]])

(defn view [conf data]
  (let [a (filter (fn [d] (= "a" (:level d))) data)
        b (filter (fn [d] (= "b" (:level d))) data)
        c (filter (fn [d] (= "c" (:level d))) data)]
    (ui/index conf (into [:div {:class "columns"}]
                         [(into [:div {:class "column"}] (map (fn [l] (card conf l)) a))
                          (into [:div {:class "column"}] (map (fn [l] (card conf l)) b))
                          (into [:div {:class "column"}] (map (fn [l] (card conf l)) c))]))))
