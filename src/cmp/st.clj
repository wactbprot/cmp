(ns cmp.st
  (:require [taoensso.carmine :as car :refer (wcar)]
            [cmp.utils :as utils])
  (:gen-class))

(def conn {:pool {} :spec {:host "127.0.0.1" :port 6379}})
(defmacro wcar* [& body] `(car/wcar conn ~@body))

(defn distrib-exchange [main-path {exchange :Exchange}]
  (map (fn [e]
         (let [{elem-path :path value :value} (utils/get-key-and-map e)]
           (def k (utils/gen-st-key [main-path "exchange" elem-path]))
           (def v (utils/gen-st-value value))
           (wcar* (car/set k v))))
       exchange))

(defn get-keys [path]
  (def k (utils/gen-st-key [path "*"]))
  (wcar* (car/keys k)))

(defn distrib [{id :_id rev :_rev mp :Mp}]
  (def p (utils/extr-main-path id))
  (clear-exchange p)
  (distrib-exchange p mp))

(defn del-keys [ks]
  (map (fn [k]
         (wcar* (car/del k)))
         ks))

(defn clear [id]
  (def p (utils/extr-main-path id))
  (def k (get-keys p))
  (del-keys k))

(defn clear-exchange [main-path]
  (def p (utils/gen-st-key [main-path "exchange"]))
  (def k (get-keys p))
  (del-keys k))
