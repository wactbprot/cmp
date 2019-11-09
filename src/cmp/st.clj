(ns cmp.st
  (:require [taoensso.carmine :as car :refer (wcar)]
            [cmp.utils :as u]
            [cmp.config :as cfg])
  (:use [clojure.repl])
  (:gen-class))

(def conn (cfg/st-conn (cfg/config)))

(defmacro wcar*
  [& body]
  `(car/wcar conn ~@body))

(defn get-keys
  [p]
  (wcar* (car/keys (u/vec->key [p "*"]))))

(defn del-keys!
  [ks]
  (doall
   (map (fn [k] (wcar* (car/del k)))
        ks)))

(defn del-key!
  [k]
  (wcar* (car/del k)))
  

(defn set-val!
  [k v]
  (wcar* (car/set k v)))

(defn get-val
  [k]
  (wcar* (car/get k)))

(defn get-vals
  [ks]
  (doall
   (mapv
    (fn [k] (get-val k))
    ks)))

(defn val->int
  [x]
  (Integer/parseInt x))
 
(defmulti clear
  class)

(defmethod clear String
  [k]
  (->> k
       (get-keys)
       (del-keys!)))

(defmethod clear clojure.lang.PersistentVector
  [k]
  (->> k
       (u/vec->key)
       (get-keys)
       (del-keys!)))