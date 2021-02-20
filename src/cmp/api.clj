(ns cmp.api
      ^{:author "wactbprot"
        :doc "api for cmp info and ctrl."}
  (:require [cmp.build               :as build]
            [cheshire.core           :as che]
            [cmp.config              :as config]
            [cmp.exchange            :as exch]
            [cmp.doc                 :as doc]
            [cmp.lt-mem              :as lt]
            [cmp.st-mem              :as st]
            [cmp.state               :as state]
            [cmp.task                :as tsk]
            [cmp.utils               :as u]
            [cmp.api-utils           :as au]
            [cmp.key-utils           :as ku]
            [com.brunobonacci.mulog  :as mu]))


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
  (mapv au/key-value-map (st/key->keys (ku/task-prefix))))

;;------------------------------
;; mp info
;;------------------------------
(defn mp-meta
  [conf req]
  (let [mp-id (au/req->mp-id req)]
    {:mp-id mp-id
     :descr   (st/key->val (ku/meta-descr-key   mp-id))
     :name    (st/key->val (ku/meta-name-key    mp-id))
     :ncont   (st/key->val (ku/meta-ncont-key   mp-id))
     :ndefins (st/key->val (ku/meta-ndefins-key mp-id))
     :std     (st/key->val (ku/meta-std-key     mp-id))
     :docs    (mapv st/key->val (st/key->keys (ku/id-prefix mp-id)))}))

;;------------------------------
;; container
;;------------------------------
(defn container-ctrl 
  [conf req]
  (let [mp-id   (au/req->mp-id req)
        no-idx  (au/req->no-idx req)]
    (mapv (fn [ck tk] (au/key-value-map ck {:run   "run"
                                            :mon   "mon"
                                            :stop  "stop"
                                            :title (st/key->val tk)}))
          (st/pat->keys (ku/cont-ctrl-key mp-id no-idx))
          (st/pat->keys (ku/cont-title-key mp-id no-idx)))))

(defn container-state
  [conf req]
  (let [mp-id   (au/req->mp-id req)
        no-idx  (au/req->no-idx req)]
    (mapv
     (fn [k] (au/key-value-map k {:ready    "ready"
                                  :working  "working"
                                  :executed "executed"
                                  :title    (st/key->val (ku/cont-title-key mp-id no-idx))}))
     (st/pat->keys (ku/cont-state-key mp-id no-idx "*" "*" )))))

(defn container-definition
  [conf req]
  (let [mp-id   (au/req->mp-id req)
        no-idx  (au/req->no-idx req)]
    (mapv (fn [k] (au/key-value-map k {:task  (tsk/build k)}))
          (st/pat->keys (ku/cont-defin-key mp-id no-idx "*" "*" )))))

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
