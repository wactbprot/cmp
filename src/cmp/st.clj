(ns cmp.st
  (:require [taoensso.carmine :as car :refer (wcar)]
            [cmp.utils :as utils])
  (:gen-class))

(def conn {:pool {} :spec {:host "127.0.0.1" :port 6379}})
(defmacro wcar* [& body] `(car/wcar conn ~@body))

(defn distrib-exchange [main-path {exchange :Exchange}]
  (def struct-path "exchange")
  (map (fn [v]
         (let [{elem-path :path value :value} (utils/get-key-and-map v)]
           (def st-key (utils/gen-st-key [main-path struct-path elem-path]))
           (def st-value (utils/gen-st-value value))
           (wcar* (car/set st-key st-value)))) exchange))
;; (wcar* (car/ping))

(defn distrib [{id :_id rev :_rev mp :Mp}]
  (def main-path (utils/extr-main-path id))
  (distrib-exchange main-path mp))

