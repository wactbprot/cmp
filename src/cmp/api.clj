(ns cmp.api
      ^{:author "wactbprot"
        :doc "api for cmp info and ctrl."}
  (:require [cheshire.core           :as che]
            [cmp.exchange            :as exch]
            [cmp.doc                 :as doc]
            [cmp.key-utils           :as ku]
            [cmp.lt-mem              :as lt]
            [cmp.st-mem              :as st]
            [cmp.utils               :as u]
            [com.brunobonacci.mulog  :as mu]))

(defn kv
  "Generates a map consisting of `k`ey and `v`alue. The result is
  merged with `m`."
  ([k]
   (kv k nil))
  ([k m]
   (merge (assoc (ku/key->info-map k) :value (st/key->val k) :key k) m)))

;;------------------------------
;; listeners 
;;------------------------------
(defn listeners
  "Returns the `reg-key` and the `id` of the running listeners.

  Example:
  ```clojure
  (listener {} {})
  ```"
  [conf req]
  (let [ls @st/listeners]
    (mapv (fn [k] (assoc (ku/key->reg-map k) :key k :listener-id (get-in ls [k :id]))) (keys ls))))

;;------------------------------
;; tasks
;;------------------------------
(defn tasks
  "Returns the `tasks` `key`-`value` pairs available at `st-mem`.

  Example:
  ```clojure
  (tasks {} {})
  ```"
  [conf req]
  (mapv kv (st/key->keys (ku/task-prefix))))

;;------------------------------
;; mp info
;;------------------------------
(defn mp-meta
  [conf req mp-id]
  {:mp-id mp-id
   :descr   (st/key->val (ku/meta-descr-key   mp-id))
   :name    (st/key->val (ku/meta-name-key    mp-id))
   :ncont   (st/key->val (ku/meta-ncont-key   mp-id))
   :ndefins (st/key->val (ku/meta-ndefins-key mp-id))
   :std     (st/key->val (ku/meta-std-key     mp-id))
   })

;;------------------------------
;; container
;;------------------------------
(defn container-ctrl 
  ([conf req mp-id]
   (container-ctrl conf req mp-id "*"))
  ([conf req mp-id no-idx]
   (mapv
    (fn [k] (kv k {:run "run" :mon "mon" :stop "stop"}))
    (st/pat->keys (ku/cont-ctrl-key mp-id no-idx)))))

(defn container-state
  ([conf req mp-id]
   (container-state conf req mp-id "*"))
  ([conf req mp-id no-idx]
   (mapv
    (fn [k] (kv k {:ready "ready" :working "working" :executed "executed"}))
    (st/pat->keys (ku/cont-state-key mp-id no-idx "*" "*" )))))

(defn container-definition
  ([conf req mp-id]
   (container-definition conf req mp-id "*"))
  ([conf req mp-id no-idx]
   (mapv kv (st/pat->keys (ku/cont-defin-key mp-id no-idx "*" "*" )))))

;;------------------------------
;; definitions
;;------------------------------
(defn mp-meta [conf req mp-id] (mapv kv (st/key->keys (ku/meta-prefix mp-id))))

(defn definitions-ctrl 
  ([conf req mp-id]
   (definitions-ctrl conf req mp-id "*"))
  ([conf req mp-id no-idx]
   (mapv
    (fn [k] (kv k {:run "run" :mon "mon" :stop "stop"}))
    (st/pat->keys (ku/defins-ctrl-key mp-id no-idx)))))

(defn definitions-state
  ([conf req mp-id]
   (definitions-state conf req mp-id "*"))
  ([conf req mp-id no-idx]
   (mapv
    (fn [k] (kv k {:ready "ready" :working "working" :executed "executed"}))
    (st/pat->keys (ku/defins-state-key mp-id no-idx "*" "*" )))))

(defn definitions-definition
  ([conf req mp-id]
   (definitions-definition conf req mp-id "*"))
  ([conf req mp-id no-idx]
   (mapv kv (st/pat->keys (ku/defins-defin-key mp-id no-idx "*" "*" )))))


;;------------------------------
;; set value to st-mem
;;------------------------------
(defn set-val!
  [conf req]
  (let [k (get-in req [:body :key])
        v (get-in req [:body :value])]
    (if (and k v)
      (if (= "OK" (st/set-val! k v))
        {:ok true}
        {:error "on attempt to set value"}) 
      {:error "missing key or value"})))


