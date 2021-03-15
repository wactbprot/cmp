(ns cmp.ui.setup
  (:require
   [cmp.config     :as config]
   [cmp.ui.index   :as ui]
   [cmp.api-utils  :as au]
   [clojure.pprint :as pp]
   [cmp.utils :as u]))
  
(defn content
  [conf]
  [:div {:class "columns"}
   [:div {:class "column"}
    [:button {:title "Clears all state, reloads tasks, reloads mpds"
             :class "button is-small setter is-danger"
             :data-value "server"
             :data-url "cmd"
              :data-key "restart"}
     "restart server"]]
   [:div {:class "column"}
    [:button {:title "Clears all tasks, reloads all tasks"
              :class "button is-small setter is-warning"
              :data-value "tasks"
              :data-url "cmd"
              :data-key "rebuild"}
     "rebuild tasks"]]])

(defn view [conf req data] (ui/index conf req (content conf)))
