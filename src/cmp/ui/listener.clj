(ns cmp.ui.listener
  (:require
     [cmp.ui.core :as ui]))

(defn level-class
  [conf m]
  {:class (condp = (:level m)
            "a" "tag is-primary"
            "b" "tag is-info"
            "tag is-info is-light")})

(defn func-class
  [conf m]
  {:class (condp = (:func m)
            "ctrl"  "tag is-dark"
            "state" "tag is-info"
            "tag is-light")})

(defn no-idx-class [conf m] {:class "tag is-light"})

(defn label
  [conf m]
  [:div {:class "control"}
   [:div {:class "tags has-addons"}
    [:span (func-class conf m) "funtion: " (:func m)]
    [:span (level-class conf m) "level: "  (:level m)]
    [:span (no-idx-class conf m) "no: "  (:no-idx m)]]])

(defn card-content
  [conf m]
  [:div {:class "card-image"}
   [:figure {:class "image is-3by1"}
    (ui/img conf m "../")]
   [:p [:b "Measurement Prog.: "] (ui/mp-id-link m)]
   (label conf m)])

(defn card
  [conf m]
  (ui/card-template conf m (card-content conf m) (ui/card-footer conf m)))

(defn view [conf req data]
  (let [a (filter (fn [d] (and (= "a" (:level d))
                               (= "ctrl"  (:func d)))) data)
        b (filter (fn [d] (= "state" (:func d))) data)]
    (ui/index conf req (into [:div {:class "columns"}]
                             [(into [:div {:class "column"}
                                     [:h5 "Listener for ctrl (a)"]] (map (fn [l] (card conf l)) a))
                              (into [:div {:class "column"}
                                     [:h5 "Listener for state (a/b)"]] (map (fn [l] (card conf l)) b))]))))
