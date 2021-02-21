(ns cmp.ui.task
  (:require
   [cmp.api-utils   :as au]
   [cheshire.core   :as che]
   [ring.util.codec :as codec]
   [cmp.utils       :as u]
   [cmp.ui.index    :as ui]))

(defn label
  [conf t]
  [:div {:class "control"}
   [:div {:class "tags has-addons"}
    [:span {:class "tag is-info"} (:Action t)]
    [:span {:class "tag is-dark"} (:TaskName t)]]])

(defn section
  [conf t body]
  [:section {:title (che/encode t)}
   [:p 
    (label conf t)
    [:i {:class "is-size-7"} (:Comment t)]]
   body])

(defmulti card  (fn [conf m] (-> m :task :Action keyword)))

(defmethod card :MODBUS
  [conf {t :task :as m}]
  (ui/card-template conf m (section conf t
                                      [:p 
                                       [:ul {:class "is-size-7"}
                                        [:li  "Host: " (:Host t)]
                                        [:li "Address: " (:Address t)]]])))

(defmethod card :TCP
  [conf {t :task :as m}]
  (ui/card-template conf m (section conf t
                                      [:p 
                                       [:ul {:class "is-size-7"}
                                        [:li "Host: " (:Host t)]
                                        [:li "Port: " (:Port t)]
                                        [:li "Value: " (:Value t)]]])))

(defmethod card :runMp
  [conf {t :task :as m}]
  (let [mp     (u/extr-main-path  (:Mp t))
        title  (:ContainerTitle t)
        txt    (str mp "/state/" title)
        href   (str "/ui/" mp "/container/state/" (codec/url-encode title))]
    (ui/card-template conf m
                      (section conf t [:p 
                                       [:ul {:class "is-size-7"}
                                        [:li [:a {:href href} txt ]]]]))))

(defmethod card :default 
  [conf {t :task :as m}]
  (ui/card-template conf m (section conf t [:div])))
