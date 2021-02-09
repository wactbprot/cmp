(ns cmp.ui.core
  (:require
     [hiccup.form :as hf]
     [hiccup.page :as hp]))

(defn page-header
  [conf]
  [:head
   [:title (:page-title conf)]
   (hp/include-css "/css/bulma.css")
   (hp/include-css "/css/all.css")])

(defn index
  [conf body]
  (hp/html5
   (page-header conf)
   [:body body
    (hp/include-js "/js/jquery-3.5.1.min.js")
    (hp/include-js "/js/ws.js")
    (hp/include-js "/js/main.js")]))
