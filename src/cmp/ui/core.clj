(ns cmp.ui.core
  (:require
     [hiccup.form :as hf]
     [hiccup.page :as hp]))

(defn empty-msg [s] [:span {:class "tag is-info"} s])

;;------------------------------
;; table funs
;;------------------------------
(defn kw-head [m] (keys (first m)))

(defn t-head  [kws] (into [:thead] (mapv (fn [x] [:th x]) kws))) 

(defn td      [m kws] (mapv (fn [kw] [:td (kw m)]) kws))

(defn t-row   [m kws] (mapv (fn [x]  (into [:tr] (td x kws))) m))

(defn t-base  [kws]   [:table {:class "table"} (t-head kws)])

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
