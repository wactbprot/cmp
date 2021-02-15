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
;; container
;;------------------------------
(defn container-title
  "Returns the `container-title` `key`-`value` pairs.
  
  Example:
  ```clojure
  (container-title {} {} \"ref\")
  ```"
  [conf req mp-id]
  (mapv kv (st/pat->keys (ku/cont-title-key mp-id "*"))))
  
(defn mp-meta [conf req mp-id] (mapv kv (st/key->keys (ku/meta-prefix mp-id))))

(defn container-descr [conf req mp-id] (mapv kv (st/pat->keys (ku/cont-descr-key mp-id "*"))))

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

(defn set-val!
  [conf req]
  (let [k (get-in req [:body :key])
        v (get-in req [:body :value])]
    (if (and k v)
      (if (= "OK" (st/set-val! k v))
        {:ok true}
        {:error "on attempt to set value"}) 
      {:error "missing key or value"})))


