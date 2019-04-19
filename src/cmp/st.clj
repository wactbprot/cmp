(ns cmp.st
  (:require [taoensso.carmine :as car :refer (wcar)]
            [cmp.utils :as utils])
  (:gen-class))

(def conn {:pool {} :spec {:host "127.0.0.1" :port 6379}})
(defmacro wcar* [& body] `(car/wcar conn ~@body))

(defn get-keys [path]
  (def k (utils/gen-st-key [path "*"]))
  (wcar* (car/keys k)))

(defn del-keys [ks]
  (map (fn [k]
         (wcar* (car/del k)))
         ks))

(defn clear [id]
  (del-keys
   (get-keys
    (utils/extr-main-path id))))

(defn clear-exchange [main-path]
  (del-keys
   (get-keys
    (utils/gen-st-key [main-path "exchange"])))
  (println "cl done")
  )

(defn distrib-exchange [main-path {exchange :Exchange}]
  (doseq [[k v] exchange]
    (wcar* (car/set
            (utils/gen-st-key [main-path "exchange" (name k)])
            (utils/gen-st-value v)))))

(defn distrib-containers [main-path {container :Container}]
  (map-indexed (fn [i c]
         (let [{description :Description
                title :Title
                ctrl :Ctrl
                elem :Element} c]
           (wcar* (car/set
                   (utils/gen-st-key [main-path "container" i "title"])
                   (str title)))
           (wcar* (car/set
                   (utils/gen-st-key [main-path "container" i "description"])
                   (str description)))
           (wcar* (car/set
                   (utils/gen-st-key [main-path "container" i "ctrl"])
                   (utils/gen-st-value ctrl)))
           (wcar* (car/set (utils/gen-st-key [main-path "container" i "elem"])
                           (utils/gen-st-value elem)))))
       container))


(defn distrib [{id :_id rev :_rev mp :Mp}]
  (let [p (utils/extr-main-path id)]
  (clear-exchange p)
  (distrib-exchange p mp)
  (distrib-containers p mp)))
