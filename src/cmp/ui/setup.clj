(ns cmp.ui.setup
  (:require
   [cmp.config     :as config]
   [cmp.ui.index   :as ui]
   [cmp.api-utils  :as au]
   [clojure.pprint :as pp]
   [cmp.utils :as u]))
  
(defn content
  [conf]
  [:div {:class "content"}
   [:h3 "Configuration"]
   [:p
    [:table {:class "table is-bordered is-hoverable is-fullwidth"}
     [:thead
      [:tr
       [:th "Item"]
       [:th "Value"] ]]
     [:tbody
      [:tr
       [:td "DEVHUB_URL"]
       [:td (config/dev-hub-url (config/config))]]
      [:tr
       [:td "DEVPROXY_URL"]
       [:td (config/dev-proxy-url (config/config))]]
      [:tr
       [:td "LT-MEM-CONN"]
       [:td (str (config/lt-conn (config/config)))]]
      [:tr
       [:td "ST-MEM-CONN"]
       [:td  (str (config/st-conn (config/config)))]]
      [:tr
       [:td "BUILD-ON-START"]
       [:td  (str (config/build-on-start (config/config)))]]]]]
   [:h3 "Danger Zone"]
   [:p   
    [:div {:class "columns"}
     [:div {:class "column"}
      [:button {:title "Clears all state, reloads tasks, reloads mpds"
                :class "button is-small restart is-danger"
               :data-value "server"
               :data-url "cmd"
               :data-key "restart"}
      "restart server"]]
    [:div {:class "column"}
     [:button {:title "Clears all tasks, reloads all tasks"
              :class "button is-small rebuild is-warning"
               :data-value "tasks"
               :data-url "cmd"
              :data-key "rebuild"}
      "rebuild tasks"]]]]])

(defn view [conf req data] (ui/index conf req (content conf)))
