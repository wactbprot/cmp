(ns cmp.st-mem
  (:require [taoensso.carmine :as car :refer (wcar)]
            [cmp.utils :as u]
            [taoensso.timbre :as timbre]
            [clojure.string :as string]
            [clojure.data.json :as json]
            [cmp.config :as cfg])
  (:use [clojure.repl]))

(def conn (cfg/st-conn (cfg/config)))


(defmulti gen-value
  class)

(defmethod gen-value clojure.lang.PersistentArrayMap
  [x]
  (json/write-str x))

(defmethod gen-value clojure.lang.PersistentVector
  [x]
  (json/write-str x))

(defmethod gen-value clojure.lang.PersistentHashMap
  [x]
  (json/write-str x))

(defmethod gen-value :default
  [x]
  x)

(defn pat->keys
  "Get all keys matching  the given pattern `pat`."
  [pat]
  (wcar conn  (car/keys pat)))

(defn get-keys
  "Get all keys matching  `p*`."
  [p]
  (pat->keys (u/vec->key [p "*"])))

(defn del-keys!
  "Deletes all given keys (`ks`)."
  [ks]
  (run!
   (fn [k] (wcar conn (car/del k)))
   ks))

(defn del-key!
  "Delets the key `k`."
  [k]
  (wcar conn (car/del k)))
  
(defn set-val!
  "Sets the value `v` for the key `k`."
  [k v]
  (wcar conn (car/set k (gen-value v))))

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

(defn filter-keys-where-val
  "Returns all keys belonging to `pat` where the
  value is `val`.

  ```clojure
  (filter-keys-where-val \"ref@definitions@*@class\" \"wait\")
  ;; (\"ref@definitions@0@class\"
  ;; \"ref@definitions@2@class\"
  ;; \"ref@definitions@1@class\")
  ```
  "
  [pat val]
  (filter (fn [k] (= (key->val k) val))
          (pat->keys pat)))

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


;;------------------------------
;; keyspace notification
;;------------------------------
(defn msg->key
  "Extracts the key from a published keyspace
  notification message (`pmessage`).

  ```clojure
  (def msg [\"pmessage\"
           \"__keyspace@0*__:ref@*@*@ctrl*\"
           \"__keyspace@0__:ref@container@0@ctrl\"
           \"set\"])
  (st/msg->key msg)
  ;; \"ref@container@0@ctrl\"
  ```"
  [[kind l1 l2 l3]]
  (timbre/debug "received" kind l1 l2 l3)
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
  (close-listener! (gen-listener \"ref\" \"ctrl\" msg->key))
  ```"
  [mp-id l2 l3 l4 callback]
  (let [subs-pat (gen-subs-pat mp-id l2 l3 l4)]
    (car/with-new-pubsub-listener (:spec conn)
      {subs-pat callback}
      (car/psubscribe subs-pat))))  

(defn close-listener!
  "Closes the given listener generated by [[gen-listener]].

  ```clojure
  ;; generate
  (def l (gen-listener \"ref\" \"ctrl\" msg->key))
  ;; close 
  (close-listener! l)
  ```"
  [l]
  (car/close-listener l))

;;------------------------------
;; listeners 
;;------------------------------
(def listeners
  "Listener has the form `(atom {})`." 
  (atom {}))

;;------------------------------
;;register!, registered?, de-register!
;;------------------------------
(defn gen-reg-key
  "generates a registration key for the listener atom."
  [mp-id struct no op]
  (str mp-id "_" struct "_" no "_" op))

(defn registered?
  "Checks if a `listener` is registered under
  the `listeners`-atom."
  [reg-key]
  (contains? (deref listeners) reg-key))

(defn register!
  "Generates a `ctrl` listener and registers him
  under the key `mp-id` in the `listeners` atom.
  The callback function dispatches depending on
  the result."
  [mp-id struct no op callback]
  (let [reg-key (gen-reg-key mp-id struct no op)]
        (cond
          (registered? reg-key) (timbre/info "a ctrl listener for "
                                             mp-id struct no op
                                             " is already registered!") 
          :else (do
                  (swap! listeners assoc
                         reg-key
                         (gen-listener mp-id struct no op callback))
                  (timbre/info "registered listener for: " mp-id struct no op)))))

(defn de-register!
  "De-registers the listener with the
  key `mp-id` in the `listeners` atom."
  [mp-id struct no op]
  (let [reg-key (gen-reg-key mp-id struct no op)]
    (cond
      (registered? reg-key) (do
                              (close-listener! ((deref listeners) reg-key))
                              (swap! listeners dissoc reg-key)
                              (timbre/info "de-registered listener: " reg-key))
      :else (timbre/info "a ctrl listener for "
                         reg-key
                         " is not registered!"))))
