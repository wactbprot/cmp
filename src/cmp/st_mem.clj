(ns cmp.st-mem
  (:require [taoensso.carmine :as car :refer (wcar)]
            [cmp.utils :as u]
            [taoensso.timbre :as log]
            [clojure.string :as string]
            [clojure.data.json :as json]
            [cmp.config :as cfg]))

(def conn (cfg/st-conn (cfg/config)))
(def db (cfg/st-db (cfg/config)))
(def mtp (cfg/min-task-period (cfg/config)))

;;------------------------------
;; store
;;------------------------------
(defn set-val!
  "Sets the value `v` for the key `k`."
  [k v]
  (if (string? k)
    (if (some? v)
      (wcar conn (car/set k (u/clj->val v)))
      (log/warn "no value given"))
    (log/warn "no key given")))

(defn set-same-val!
  "Sets the given `val` for all keys `ks` with the delay `mtp`."
  [ks v]
  (run!
   (fn [k]
     (set-val! k v))
   ks))

;;------------------------------
;; set state
;;------------------------------
(defn set-state!
  "Function is used by the workers to set state. An optional log message
  may be provided."
  ([k state msg]
   (condp = state
     :error (log/error msg)
     :ready (log/debug msg)
     (log/debug msg))
   (set-state! k state))
  ([k state]
   (when (and (string? k)
              (keyword? state)) 
     (set-val! k (name state))
     (log/debug "wrote new state: " state " to: " k)
     (when (= state :working) (Thread/sleep mtp)))))

(defn pat->keys
  "Get all keys matching  the given pattern `pat`."
  [pat]
  (sort (wcar conn (car/keys pat))))

(defn key->keys
  "Get all keys matching  `k*`."
  [k]
  (pat->keys (u/vec->key [k "*"])))

;;------------------------------
;; del
;;------------------------------
(defn del-key!
  "Delets the key `k`."
  [k]
  (wcar conn (car/del k)))

(defn del-keys!
  "Deletes all given keys (`ks`)."
  [ks]
  (run! del-key! ks))

(defn clear
  "Clears the key `x`. If `x` is a vector the function `u/vec->key` is
  used for the conversion of `x` to a string."
  [x]
  (condp = (class x)
    String                        (->> x
                                        key->keys
                                        del-keys!)
    clojure.lang.PersistentVector (->> x
                                       u/vec->key
                                       key->keys
                                       del-keys!)))

;;------------------------------
;; get value(s)
;;------------------------------
(defn key->val
  "Returns the value for the given key (`k`) and cast it to a clojure
  type."
  [k]
  (u/val->clj (wcar conn (car/get k))))

(defn keys->vals
  "Returns a vector of the `vals` behind the keys `ks`."
  [ks]
  (mapv key->val ks))

(defn filter-keys-where-val
  "Returns a list of all keys belonging to the pattern `pat` where the
  value is equal to`x`.
  
  Example:
  ```clojure
  (filter-keys-where-val \"ref@definitions@*@class\" \"wait\")
  ;; (\"ref@definitions@0@class\"
  ;; \"ref@definitions@2@class\"
  ;; \"ref@definitions@1@class\")
  ```
  "
  [pat x]
  (filter
   (fn [k] (= x (key->val k)))
   (pat->keys pat)))

;;------------------------------
;; keyspace notification
;;------------------------------
(defn msg->key
  "Extracts the `key` from a published keyspace notification
  message (`pmessage`).

  Example:
  ```clojure
  (def msg [\"pmessage\"
           \"__keyspace@0*__:ref@*@*@ctrl*\"
           \"__keyspace@0__:ref@container@0@ctrl\"
           \"set\"])
  (st/msg->key msg)
  ;; \"ref@container@0@ctrl\"
  ```"
  [[kind l1 l2 l3]]
  (condp = (keyword kind)
    :pmessage   (second (string/split l2 #":"))
    :psubscribe (log/debug "subscribed to " l1)
    (log/warn "received" kind l1 l2 l3)))

(defn subs-pat
  "Generates subscribe patterns which matches depending on:
  
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
  (str "__keyspace@" db "*__:" mp-id
       u/sep l2
       u/sep l3
       u/sep l4 "*"))

(defn gen-listener
  "Returns a listener for published keyspace notifications. Don't forget
  to [[close-listener!]]

  Example:
  ```clojure
  ;; generate and close
  (close-listener! (gen-listener \"ref\" \"ctrl\" msg->key))
  ```"
  [mp-id l2 l3 l4 callback]
  (let [pat (subs-pat mp-id l2 l3 l4)]
    (car/with-new-pubsub-listener (:spec conn)
      {pat callback}
      (car/psubscribe pat))))  

(defn close-listener!
  "Closes the given listener generated by [[gen-listener]].

  Example:
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
(defonce listeners (atom {}))

;;------------------------------
;;register!, registered?, de-register!
;;------------------------------
(defn reg-key
  "Generates a registration key for the listener atom.
  The `level` param allows to register more than one listener for one
  pattern."
  [mp-id struct no func level]
  (str mp-id "_" struct "_" no "_" func "_" level))

(defn registered?
  "Checks if a `listener` is registered under
  the `listeners`-atom."
  [k]
  (contains? (deref listeners) k))

(defn register!
  "Generates and registers a listener under the key `mp-id` in the
  `listeners` atom.  The cb! function dispatches depending on the
  result."
  ([mp-id struct no func cb!]
   (register! mp-id struct no func cb! "a"))
  ([mp-id struct no func cb! level]
   (let [reg-key (reg-key mp-id struct no func level)]
     (if-not (registered? reg-key)
       {:ok (map?
             (swap! listeners assoc
                    reg-key
                    (gen-listener mp-id struct no func cb!)))}
       {:ok true :warn "already registered"}))))

(defn de-register!
  "De-registers the listener with the key `mp-id` in the `listeners`
  atom."
  ([mp-id struct no func]
   (de-register! mp-id struct no func "a"))
  ([mp-id struct no func level]
   (let [k (reg-key mp-id struct no func level)]
     (if (registered? k)
       (do
         (log/debug "de-register:" reg-key)
         (close-listener! ((deref listeners) k))
         {:ok (map? (swap! listeners dissoc k))})
       {:ok true :warn "not registered"}))))

(defn clean-register!
  "Closes and `de-registers!` all `listeners` belonging to `mp-id` ."
  [mp-id]
  (map (fn [[k v]]
         (if (string/starts-with? k mp-id)
           (do
             (close-listener! v)
             {:ok (map? (swap! listeners dissoc k))})
           {:ok true :reason "unrelated"}))
       (deref listeners)))
