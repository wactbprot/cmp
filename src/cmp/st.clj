(ns cmp.st
  (:require [taoensso.carmine :as car :refer (wcar)]
            [cmp.utils :as utils])
  (:gen-class))

(def conn {:pool {} :spec {:host "127.0.0.1" :port 6379}})
(defmacro wcar* [& body] `(car/wcar conn ~@body))

(defn get-keys [path]
  (wcar* (car/keys (utils/gen-st-key [path "*"]))))

(defn del-keys [ks]
  (map (fn [k]
         (wcar* (car/del k)))
         ks))

(defn clear [id]
  (del-keys
   (get-keys
    (utils/extr-main-path id))))

(defn clear-sub [main-path sup-path]
  (del-keys
   (get-keys
    (utils/gen-st-key [main-path sup-path]))))

(defn distrib-exchange [m-path {exchange :Exchange}]
  (def e-path "exchange")
  (doseq [[k v] exchange]
    (wcar* (car/set
            (utils/gen-st-key [m-path e-path (name k)])
            (utils/gen-st-value v)))))

(defn distrib-containers [m-path {container :Container}]
  (def e-path "container")
  (map-indexed (fn [i c]
         (let [{description :Description
                title :Title
                ctrl :Ctrl
                elem :Element} c]
           (wcar* (car/set
                   (utils/gen-st-key [m-path e-path i "title"])
                   (str title)))
           (wcar* (car/set
                   (utils/gen-st-key [m-path e-path i "description"])
                   (str description)))
           (wcar* (car/set
                   (utils/gen-st-key [m-path e-path i "ctrl"])
                   (utils/gen-st-value ctrl)))
           (wcar* (car/set (utils/gen-st-key [m-path e-path i "elem"])
                           (utils/gen-st-value elem)))))
       container))


(defn distrib [{id :_id rev :_rev mp-def :Mp}]
  (let [m-path (utils/extr-main-path id)]
  (clear-sub m-path "exchange")
  (distrib-exchange m-path mp-def)
  (clear-sub m-path "container")
  (distrib-containers m-path mp-def)))
