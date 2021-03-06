(ns cmp.ui.task
  (:require
   [cheshire.core       :as che]
   [ring.util.codec     :as codec]
   [cmp.utils           :as u]
   [cmp.ui.index        :as ui]))

(defn label
  [conf t]
  [:div {:class "control"}
   [:div {:class "tags has-addons"}
    [:span {:class "tag is-info"} (:Action t)]
    [:span {:class "tag is-light is-info"} (:TaskName t)]]])

(defn div
  [conf t body]
  [:div {:title (che/encode t)}
   (label conf t)
   [:div {:class "mb-0 notification is-light is-info"} (:Comment t)]
   body])

(defmulti card  (fn [conf m] (-> m :task :Action keyword)))

(defmethod card :MODBUS
  [conf {t :task :as m}]
  (div conf t
       [:ul 
        [:li  "Host: " (:Host t)]
        [:li "Address: " (:Address t)]]))

(defmethod card :TCP
  [conf {t :task :as m}]
  (div conf t
       [:ul
        [:li "Host: " (:Host t)]
        [:li "Port: " (:Port t)]
        [:li "Value: " (:Value t)]]))

(defmethod card :select
  [conf {t :task :as m}]
  (div conf t
       [:ul
        [:li "DefinitionClass: " (:DefinitionClass t)]]))

(defmethod card :runMp
  [conf {t :task :as m}]
  (let [mp     (u/extr-main-path  (:Mp t))
        title  (:ContainerTitle t)
        txt    (str mp "/state/" title)
        href   (str "/ui/" mp "/container/" (codec/url-encode title))]
    (div conf t [:ul 
                 [:li [:a {:href href} txt ]]])))

(defmethod card :default 
  [conf {t :task :as m}]
   (div conf t [:div]))
