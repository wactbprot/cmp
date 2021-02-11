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

(defn kv [k] (assoc (ku/key->info-map k) :value (st/key->val k) :key k))

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
    (mapv (fn [k] (assoc (ku/key->reg-map k) :listener-id (get-in ls [k :id]))) (keys ls))))

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
  

(defn container-descr [conf req mp-id] (mapv kv (st/pat->keys (ku/cont-descr-key mp-id "*"))))

(defn container-ctrl  [conf req mp-id] (mapv kv (st/pat->keys (ku/cont-ctrl-key mp-id "*"))))

(defn container-state [conf req mp-id] (mapv kv (st/pat->keys (ku/cont-state-key mp-id "*" "*" "*" ))))
(defn container-definition [conf req mp-id] (mapv kv (st/pat->keys (ku/cont-defin-key mp-id "*" "*" "*" ))))

(defn mp-meta         [conf req mp-id] (mapv kv (st/key->keys (ku/meta-prefix mp-id))))

