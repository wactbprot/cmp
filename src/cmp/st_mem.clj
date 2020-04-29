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

(defn set-val!
  "Sets the value `v` for the key `k`."
  [k v]
  (if k
    (if v
      (wcar conn (car/set k (u/clj->val v)))
      (timbre/warn "no value given"))
    (timbre/warn "no key given")))

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
  "Clears the key `x`. If `x` is a vector
  the function `u/vec->key` is used for
  the conversion of `x` to a string."
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
  "Returns the value for the given key (`k`)
  and cast it to a clojure type."
  [k]
  (u/val->clj (wcar conn (car/get k))))

(defn keys->vals
  "Returns a vector of the `vals`
  behind the keys `ks`."
  [ks]
  (into [] (map key->val ks)))

(defn filter-keys-where-val
  "Returns a list of all keys belonging
  to the pattern `pat` where the value
  is equal to`x`.
  
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


(defn state-key->response-key
  "Turns the given `state-key` into a
  `response-key`.

  ```clojure
  (state-key->response-key \"devs@container@0@state@0@0\")
  ;; devs@container@0@response@0@0
  ```
  "
  [state-key]
  (u/replace-key-at-level 3 state-key "response"))

;;------------------------------
;; key arithmetic
;;------------------------------
(defn key->key-space
  "Returns the name of the key space for
  the given key.

  May be:
  * tasks
  * <mp-id>

  "
  [k]
  {:pre [(string? k)]}
  (nth (string/split k u/re-sep) 0 nil))

(defn key->struct
  "Returns the name of the `struct`ure
  for the given key.

  May be:
  * <taskname>
  * definitions
  * container
  "
  [k]
  {:pre [(string? k)]}
  (nth (string/split k u/re-sep) 1 nil))


(defn key->no-idx
  "Returns an integer corresponding to the
  given key `container` or `definitions` index."
  [k]
  {:pre [(string? k)]}
  (if-let [n (nth (string/split k u/re-sep) 2 nil)]
    (Integer/parseInt n)))

(defn key->func
  "Returns the name of the `func`tion
  for the given key."
  [k]
  {:pre [(string? k)]}
  (nth (string/split k u/re-sep) 3 nil))

(defn key->seq-idx
  "Returns an integer corresponding to
  the givens key sequential index."
  [k]
  {:pre [(string? k)]}
  (if-let [n (nth (string/split k u/re-sep) 4 nil)]
    (Integer/parseInt  n)))

(defn key->par-idx
  "Returns an integer corresponding to
  the givens key parallel index."
  [k]
  {:pre [(string? k)]}
  (if-let [n (nth (string/split k u/re-sep) 5 nil)]
    (Integer/parseInt  n)))

;;------------------------------
;; exchange
;;------------------------------
(defn exch-prefix
  "Returns the `exchange` prefix."
  [mp-id]
  (u/vec->key [mp-id "exchange"]))

(defn exch-path
  "Returns the `exchange` path (key)."
  [mp-id s]
  (u/vec->key [(exch-prefix mp-id) s]))

;;------------------------------
;; container path
;;------------------------------
(defn cont-prefix
  "Returns the `container` prefix."
  [mp-id]
  (u/vec->key [mp-id "container"]))

(defn cont-title-path
  [mp-id i]
  (u/vec->key [(cont-prefix mp-id) i  "title"]))

(defn cont-descr-path
  [mp-id i]
  (u/vec->key [(cont-prefix mp-id) i  "descr"]))

(defn cont-ctrl-path
  [mp-id i]
  (u/vec->key [(cont-prefix mp-id) i  "ctrl"]))

(defn cont-elem-path
  [mp-id i]
  (u/vec->key [(cont-prefix mp-id) i  "elem"]))

(defn cont-defin-path
  ([mp-id i]
   (u/vec->key [(cont-prefix mp-id) i "definition"]))
  ([mp-id i j k]
   (u/vec->key [(cont-prefix mp-id) i "definition" j k])))

(defn cont-state-path
  ([mp-id i]
   (u/vec->key [(cont-prefix mp-id) i  "state"]))
  ([mp-id i j k]
   (u/vec->key [(cont-prefix mp-id) i  "state" j k])))

;;------------------------------
;; definitions path
;;------------------------------
(defn defins-prefix
  "Returns the `definitions` prefix."
  [mp-id]
  (u/vec->key [mp-id "definitions"]))

(defn defins-defin-path
  ([mp-id i]
   (u/vec->key [(defins-prefix mp-id) i "definition"]))
  ([mp-id i j k]
  (u/vec->key [(defins-prefix mp-id) i "definition" j k])))

(defn defins-state-path
  ([mp-id i]
   (u/vec->key [(defins-prefix mp-id) i "state"]))
  ([mp-id i j k]
   (u/vec->key [(defins-prefix mp-id) i "state" j k])))

(defn defins-cond-path
  [mp-id i j]
  (u/vec->key [(defins-prefix mp-id) i "cond" j]))

(defn defins-ctrl-path
  [mp-id i]
  (u/vec->key [(defins-prefix mp-id) i "ctrl"]))

(defn defins-descr-path
  [mp-id i]
  (u/vec->key [(defins-prefix mp-id) i "descr"]))

(defn defins-class-path
  [mp-id i]
  (u/vec->key [(defins-prefix mp-id) i "class"]))

;;------------------------------
;; id path and pat
;;------------------------------
(defn id-prefix
  "Returns the `id` prefix."
  [mp-id]
  (u/vec->key [mp-id "id"]))

(defn id-path
  "Returns the `id` key."
  [mp-id id]
  (u/vec->key [(id-prefix mp-id) id]))

;;------------------------------
;; meta path
;;------------------------------
(defn meta-prefix
  "Returns the `meta` prefix."
  [mp-id]
  (u/vec->key [mp-id "meta"]))

(defn meta-std-path
  [mp-id]
  (u/vec->key [(meta-prefix mp-id) "std"]))

(defn meta-name-path
  [mp-id]
  (u/vec->key [(meta-prefix mp-id) "name"]))

(defn meta-descr-path
  [mp-id]
  (u/vec->key [(meta-prefix mp-id) "descr"]))

(defn meta-ncont-path
  [mp-id]
  (u/vec->key [(meta-prefix mp-id) "ncont"]))

(defn meta-ndefins-path
  [mp-id]
  (u/vec->key [(meta-prefix mp-id) "ndefins"]))

;;------------------------------
;; keyspace notification
;;------------------------------
(defn msg->key
  "Extracts the `key` from a published keyspace
  notification message (`pmessage`).

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
  (timbre/debug "msg->key received: " (str kind l1 l2 l3))
  (condp = (keyword kind)
    :pmessage   (second (string/split l2 #":"))
    :psubscribe (timbre/info "subscribed to " l1)
    (timbre/warn "received" kind l1 l2 l3)))

(defn subs-pat
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

  Example:
  ```clojure
  ;; generate and close
  (close-listener! (gen-listener \"ref\" \"ctrl\" msg->key))
  ```"
  [mp-id l2 l3 l4 callback]
  (let [s-pat (subs-pat mp-id l2 l3 l4)]
    (timbre/debug "new pubsub-listener for pat: " s-pat)
    (car/with-new-pubsub-listener (:spec conn)
      {s-pat callback}
      (car/psubscribe s-pat))))  

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
  "Generates a registration key for the listener atom."
  [mp-id struct no func]
  (str mp-id "_" struct "_" no "_" func))

(defn registered?
  "Checks if a `listener` is registered under
  the `listeners`-atom."
  [reg-key]
  (contains? (deref listeners) reg-key))

(defn register!
  "Generates and registers a  listener under the
  key `mp-id` in the `listeners` atom.
  The callback function dispatches depending on
  the result."
  [mp-id struct no func callback]
  (let [reg-key (reg-key mp-id struct no func)]
    (if-not (registered? reg-key)
      {:ok (map?
            (swap! listeners assoc
                   reg-key
                   (gen-listener mp-id struct no func callback)))}
      {:ok true :warn "already registered"})))
  
(defn de-register!
  "De-registers the listener with the
  key `mp-id` in the `listeners` atom."
  [mp-id struct no func]
  (let [reg-key (reg-key mp-id struct no func)]
    (if (registered? reg-key)
      (do
        (timbre/debug "de-register:" reg-key)
        (close-listener! ((deref listeners) reg-key))
        {:ok (map?
              (swap! listeners dissoc
                     reg-key))})
      {:ok true :warn "not registered"})))
