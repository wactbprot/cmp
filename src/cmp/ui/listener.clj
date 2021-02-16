(ns cmp.ui.listener
  (:require
     [cmp.ui.core :as ui]))

(defn label
  [conf m]
  (let [cl (condp = (keyword (:level m))
            :a "tag is-primary"
            :b "tag is-info"
            :c "tag is-info is-light")
        cf (condp = (keyword (:func m))
             :ctrl "tag is-dark"
             :state "tag is-info"
             "tag is-light")]
    [:div {:class "control"}
     [:div {:class "tags has-addons"}
      [:span {:class cf} "funtion: " (:func m)]
      [:span {:class cl} "level: "  (:level m)]
      [:span {:class "tag is-light"} "no: "  (:no-idx m)]]]))

(defn card
  [conf m]
  [:div {:class "content"}
  [:div {:class "card"}
   [:div {:class "card-image"}
    [:figure {:class "image is-3by1"}
     (ui/img conf m "../")]
   
   [:div {:class "card-content"}
    [:div {:class "content"}
     [:p {:class " is-8"}  [:b "Measurement Prog.: "] (ui/mp-id-link m)]
     (label conf m)]]]]])

(defn view [conf data]
  (let [a (filter (fn [d] (= "ctrl" (:func d))) data)
        b (filter (fn [d] (= "state" (:func d))) data)
        c (filter (fn [d] (= "c" (:level d))) data)]
    (ui/index conf (into [:div {:class "columns"}]
                         [(into [:div {:class "column"}] (map (fn [l] (card conf l)) a))
                          (into [:div {:class "column"}] (map (fn [l] (card conf l)) b))
                          (into [:div {:class "column"}] (map (fn [l] (card conf l)) c))
                          ]))))
