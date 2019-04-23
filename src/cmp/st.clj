(ns cmp.st
  (:require [taoensso.carmine :as car :refer (wcar)]
            [cmp.utils :as u])
  (:gen-class))

(def conn {:pool {} :spec {:host "127.0.0.1" :port 6379}})
(defmacro wcar* [& body] `(car/wcar conn ~@body))

(defn get-keys [path]
  (wcar* (car/keys
          (u/gen-key [path "*"]))))

(defn del-keys [ks]
  (map (fn [k]
         (wcar* (car/del k)))
         ks))

(defn set-val  [k v]
  (wcar* (car/set k v)))

(defn get-val  [k]
  (wcar* (car/get k)))

(defmulti clear class)
(defmethod clear String [k]
  (->> k
       (get-keys)
       (del-keys)))

(defmethod clear clojure.lang.PersistentVector [k]
  (->> k
       (u/gen-key)
       (get-keys)
       (del-keys)))

