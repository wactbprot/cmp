(ns cmp.st-mem
  (:require [taoensso.carmine :as car :refer (wcar)]
            [cmp.utils :as u]
            [cmp.config :as cfg])
  (:use [clojure.repl]))

(def conn (cfg/st-conn (cfg/config)))

(defmacro wcar*
  [& body]
  `(car/wcar conn ~@body))

(defn get-keys
  [p]
  (wcar* (car/keys (u/vec->key [p "*"]))))

(defn del-keys!
  "Deletes all given keys (`ks`)."
  [ks]
  (run!
   (fn [k] (wcar* (car/del k)))
   ks))

(defn del-key!
  [k]
  (wcar* (car/del k)))
  
(defn set-val!
  [k v]
  (wcar* (car/set k v)))

(defn set-same-val!
  "Sets the given values (`val`) for all keys (`ks`)."
  [ks v]
  (run!
   (fn [k] (wcar* (car/set k v)))
   ks))

(defn key->val
  "Returns the value for the given key (`k`)."
  [k]
  (wcar* (car/get k)))
 
(defmulti clear
  "Clears the key `k`. If `k` is a vector `(u/vec->key k)`
  is used for the conversion to a string."
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

;; prepair for poll elimination
(defn obs
  [msg]
  (let [[_ _ pkey _] msg]
    (println "......")
    (println pkey)))

(def listener
  (car/with-new-pubsub-listener (:spec conn)
    {"__keyspace@0*__:wait*" obs}
    (car/psubscribe "__keyspace@0*__:wait*")))

(car/close-listener listener)