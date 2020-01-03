(ns cmp.st-mem
  (:require [taoensso.carmine :as car :refer (wcar)]
            [cmp.utils :as u]
            [taoensso.timbre :as timbre]
            [clojure.string :as string]
            [cmp.config :as cfg])
  (:use [clojure.repl]))

(def conn (cfg/st-conn (cfg/config)))

(defn get-keys
  "Get all keys matching  `p*`."
  [p]
  (wcar conn  (car/keys (u/vec->key [p "*"]))))

(defn del-keys!
  "Deletes all given keys (`ks`)."
  [ks]
  (run!
   (fn [k] (wcar conn (car/del k)))
   ks))

(defn del-key!
  [k]
  (wcar conn (car/del k)))
  
(defn set-val!
  "Sets the value `v` for the key `k`."
  [k v]
  (wcar conn (car/set k v)))

(defn set-same-val!
  "Sets the given values (`val`) for all keys (`ks`)."
  [ks v]
  (run!
   (fn [k] (wcar conn  (car/set k v)))
   ks))

(defn key->val
  "Returns the value for the given key (`k`)."
  [k]
  (wcar conn (car/get k)))
 
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

(defn msg->key
  "Extracts the key from a published keyspace
  notification message.

  ```clojure
  (def msg [\"pmessage\"
           \"__keyspace@0*__:wait@*@*@ctrl*\"
           \"__keyspace@0__:wait@container@0@ctrl\"
           \"set\"])
  (st/msg->key msg)
  ;; \"wait@container@0@ctrl\"
  ```"
  [[kind l1 l2 l3]]
    (cond
      (= kind "pmessage") (second (string/split l2 (re-pattern ":")))
      (= kind "psubscribe") (timbre/info "subscribed to " l1)))
  
(defn gen-subs-pat
  "Generates subscribe patterns which matches
  depending on:

  **l2**

  * `container`
  * `definitions`

  **l3**

  * `0` ... `n`

  **l4**
  
  * `ctrl`
  * `state`
  * `definition`
  "
  [mp-id l2 l3 l4]
  (str "__keyspace@0*__:" mp-id
       u/sep l2
       u/sep l3
       u/sep l4 "*"))

(defn gen-listener
  "Returns a listener for published keyspace
  notifications. Don't forget to [[close-listener!]]
  
  ```clojure
  ;; generate and close
  (close-listener! (gen-listener \"wait\" \"ctrl\" msg->key))
  ```"
  [mp-id l2 l3 l4 cb]
  (let [subs-pat (gen-subs-pat mp-id l2 l3 l4)]
    (car/with-new-pubsub-listener (:spec conn)
      {subs-pat cb}
      (car/psubscribe subs-pat))))  

(defn close-listener!
  "Closes the given listener generated by [[gen-listener]].

  ```clojure
  ;; generate
  (def l (gen-listener \"wait\" \"ctrl\" msg->key))
  ;; close 
  (close-listener! l)
  ```"
  [l]
  (car/close-listener l))