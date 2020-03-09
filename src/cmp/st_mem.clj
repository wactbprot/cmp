(ns cmp.st-mem
  (:require [taoensso.carmine :as car :refer (wcar)]
            [cmp.utils :as u]
            [taoensso.timbre :as timbre]
            [clojure.string :as string]
            [clojure.data.json :as json]
            [cmp.config :as cfg]))

(def conn (cfg/st-conn (cfg/config)))

;;------------------------------
;; store
;;------------------------------
(defn clj->val
  "Casts the given (complex) value `x` to a writable
  type. `json` is used for complex data types.

  ```clojure
  (st/clj->val {:foo \"bar\"})
  ;; \"{\"foo\":\"bar\"}\"
  (st/clj->val [1 2 3])
  ;; \"[1,2,3]\"
  ```
  "
  [x]
  (condp = (class x)
    clojure.lang.PersistentArrayMap (json/write-str x)
    clojure.lang.PersistentVector   (json/write-str x)
    clojure.lang.PersistentHashMap  (json/write-str x)
    x))

(defn set-val!
  "Sets the value `v` for the key `k`."
  [k v]
  (wcar conn (car/set k (clj->val v))))

(defn set-same-val!
  "Sets the given values (`val`) for all keys (`ks`)."
  [ks v]
  (run!
   (fn [k] (set-val! k v))
   ks))

(defn pat->keys
  "Get all keys matching  the given pattern `pat`."
  [pat]
  (wcar conn  (car/keys pat)))

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
  "Clears the key `k`. If `k` is a vector `(u/vec->key k)`
  is used for the conversion to a string."
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
;; pick
;;------------------------------
(defn val->clj
  "Parses value `v` and returns a
  clojure type of it.

  ```clojure
  (val->clj \"-1e-9\")
  ;; -1.0E-9
  ;; class:
  ;;
  (class (val->clj \"1.23\"))
  ;; java.lang.Double
  (class (val->clj \"a\"))
  ;; java.lang.String
  (class (val->clj \"[]\"))
  ;; clojure.lang.PersistentVector
  (class (val->clj \"{}\"))
  ;; clojure.lang.PersistentArrayMap
  (class (val->clj \"{\"a\":1}\"))
  ;; clojure.lang.PersistentArrayMap
  ```
  "
  [v]
  (cond
    (nil? v) nil
    (re-find #"^-?\d+\.?\d*([Ee]\+\d+|[Ee]-\d+|[Ee]\d+)?$" v) (read-string v)
    (re-find #"^[\[\{]" v) (u/json->map v)
    :else v))

(defn key->val
  "Returns the value for the given key (`k`)
  and cast it to a clojure type."
  [k]
  (val->clj (wcar conn (car/get k))))

(defn filter-keys-where-val
  "Returns all keys belonging to `pat` where the
  value is `x`.

  ```clojure
  (filter-keys-where-val \"ref@definitions@*@class\" \"wait\")
  ;; (\"ref@definitions@0@class\"
  ;; \"ref@definitions@2@class\"
  ;; \"ref@definitions@1@class\")
  ```
  "
  [pat x]
  (filter (fn [k] (= (key->val k) x))
          (pat->keys pat)))

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
  (condp = kind
    "pmessage"   (do
                   (let [k (second (string/split l2 (re-pattern ":")))]
                     (timbre/debug "st_mem triggered key: " k)
                     k))
    "psubscribe" (timbre/info "subscribed to " l1)
    (timbre/warn "received" kind l1 l2 l3)))

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
