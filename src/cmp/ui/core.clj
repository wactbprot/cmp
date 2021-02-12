(ns cmp.ui.core
  (:require
     [hiccup.form    :as hf]
     [hiccup.page    :as hp]
     [cmp.key-utils  :as ku]
     [clojure.string :as string]
     [cheshire.core  :as che]
     ))

(defn empty-msg [s] [:span {:class "tag is-info"} s])

(defn make-selectable [k] (string/replace k ku/re-sep "_")) 
;;------------------------------
;; table cell funs
;;------------------------------
(defmulti td-value  (fn [m kw] kw))

(defmethod td-value :mp-id    [m kw] [:i (kw m)])

(defmethod td-value :no-idx   [m kw] [:i (kw m)])

(defmethod td-value :par-idx  [m kw] [:i (kw m)])

(defmethod td-value :seq-idx  [m kw] [:i (kw m)])

(defmethod td-value :level    [m kw] [:i (kw m)])

(defmethod td-value :struct   [m kw] [:b (kw m)])

(defmethod td-value :func     [m kw] [:b (kw m)])

(defmethod td-value :TaskName [m kw] [:span {:class "tag"} m])

(defmethod td-value :Replace  [m kw] [:pre (che/encode m {:pretty true})])

(defmethod td-value :Use      [m kw] [:pre (che/encode m {:pretty true})])

(defmethod td-value :key
  [m kw]
  [:span {:class "icon"}
   [:a  {:class "copy is-link fas fa-key" :data-copy (kw m)}]])

(defmethod td-value :default
  [m kw]
  (if-let [x (kw m)]
    (cond
      (boolean? x) [:b x]
      (string? x)  [:i {:id (make-selectable (:key m))} x]
      (map?    x)  (into [:ul] (mapv (fn [[k v]] [:li [:span (td-value v k)]]) x)))
    [:span  {:class "tag"} "::"]))

;;------------------------------
;; table funs
;;------------------------------
(defn kw-head [m] (keys (first m)))

(defn t-head  [kws] (into [:thead] (mapv (fn [x] [:th x]) kws))) 

(defn td      [m kws] (mapv (fn [kw] [:td (td-value m kw)]) kws))

(defn t-row   [m kws] (mapv (fn [x] (into [:tr] (td x kws))) m))

(defn t-base  [kws]
  [:table {:class "table is-hoverable is-fullwidth"}
   (t-head kws)])

(defn table
  [conf data]
  (if (empty? data) (empty-msg "no table data")
      (let [h (kw-head data)]
        (into (t-base h) (t-row data h)))))

;;------------------------------
;; page funs
;;------------------------------
(defn page-header
  [conf]
  [:head
   [:title (:page-title conf)]
   (hp/include-css "/css/bulma.css")
   (hp/include-css "/css/all.css")])

(defn index-title
  [conf]
  [:section {:class "hero is-info"}
   [:div {:class "hero-body"}
      [:div {:class "container"}
       [:h1 {:class "title"} (:main-title conf)]
       [:h2 {:class "subtitle"} "###"]]]])

(defn index
  [conf body]
  (hp/html5
   (page-header conf)
   [:body
    (index-title conf)
    [:section {:class "section"}
     [:div {:class "container content"}
      [:div {:class "box"}
       [:div {:class "columns"}
        body]]]]
    (hp/include-js "/js/jquery-3.5.1.min.js")
    (hp/include-js "/js/ws.js")
    (hp/include-js "/js/main.js")]))
