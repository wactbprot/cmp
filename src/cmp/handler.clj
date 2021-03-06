(ns cmp.handler
      ^{:author "wactbprot"
        :doc "api for cmp info and ctrl."}
  (:require [cmp.config              :as config]
            [cheshire.core           :as che]
            [cmp.exchange            :as exch]
            [cmp.task                :as tsk]
            [cmp.utils               :as u]
            [cmp.handler-utils       :as hu]
            [cmp.st-mem              :as st]
            [cmp.st-utils            :as stu]
            [com.brunobonacci.mulog  :as mu]))
;;------------------------------
;; elements (ux)
;;------------------------------
(defn elements
  "Returns the elements (-> elements from the exchange interface which should be
  accessible to the user) related to the given container."
  [conf req]
  (let [mp-id (hu/req->mp-id req)
        n     (st/key->val (stu/meta-ncont-key mp-id))]
    (mapv (fn [no-idx]
            (let [elem-keys-vec (st/key->val (stu/cont-elem-key mp-id no-idx))
                  elem-values   (mapv (fn [k] (exch/read! mp-id k)) elem-keys-vec)]
              {:mp-id         mp-id
               :no-idx        no-idx
               :title         (st/key->val (stu/cont-title-key mp-id no-idx))
               :elem-keys-vec elem-keys-vec
               :elem-values   elem-values}))
          (range n))))

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
    (mapv (fn [k] (assoc (stu/key->reg-map k) :key k :listener-id (get-in ls [k :id]))) (keys ls))))

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
  (mapv hu/key-value-map (st/key->keys (stu/task-prefix))))

;;------------------------------
;; mp info
;;------------------------------
(defn mp-meta
  [conf req]
  (let [mp-id (hu/req->mp-id req)]
    {:mp-id mp-id
     :descr   (st/key->val (stu/meta-descr-key   mp-id))
     :name    (st/key->val (stu/meta-name-key    mp-id))
     :ncont   (st/key->val (stu/meta-ncont-key   mp-id))
     :ndefins (st/key->val (stu/meta-ndefins-key mp-id))
     :std     (st/key->val (stu/meta-std-key     mp-id))
     :docs    (mapv st/key->val (st/key->keys (stu/id-prefix mp-id)))}))

;;------------------------------
;; container
;;------------------------------
(defn container
  "Gets informaton about a container out of the `st-mem`.
  
  Example:
  ```clojure
  (a/container (config/config) {:route-params  {:mp \"ref\"}})
  ```"
  [conf req]
  (let [mp-id      (hu/req->mp-id req)
        no-idx     (hu/req->no-idx req)
        seq-idx    (hu/req->seq-idx req)
        state-keys (st/pat->keys (stu/cont-state-key mp-id no-idx seq-idx "*" ))
        defin-keys (st/pat->keys (stu/cont-defin-key mp-id no-idx seq-idx "*" ))]
    (mapv (fn [sk dk]
            (let [no-idx       (stu/key->no-idx sk)
                  seq-idx      (stu/key->seq-idx sk)
                  par-idx      (stu/key->par-idx sk)

                  seq-par-sum (+ (u/ensure-int seq-idx)
                                 (u/ensure-int par-idx))                  
                  ctrl-key     (stu/cont-ctrl-key mp-id no-idx)
                  title-key    (stu/cont-title-key mp-id no-idx)]
              {:mp-id     mp-id
               :no-idx    no-idx
               :seq-idx   seq-idx
               :par-idx   par-idx
               :key       dk
               :state-key sk
               :ctrl-key  ctrl-key
               :run       (when (zero? seq-par-sum) "run")
               :suspend   (when (zero? seq-par-sum) "suspend")
               :mon       (when (zero? seq-par-sum) "mon")
               :stop      (when (zero? seq-par-sum) "stop")
               :ctrl      (when (zero? seq-par-sum) (st/key->val ctrl-key))
               :title     (when (zero? seq-par-sum) (st/key->val title-key))
               :ready     "ready"
               :working   "working"
               :executed  "executed"
               :state     (st/key->val sk)
               :task      (tsk/build dk)}))
          state-keys defin-keys)))

;;------------------------------
;; simple set value to st-mem
;;------------------------------
(defn set-val! [conf req]
  (let [k (hu/req->key req) 
        v (hu/req->value req)]
    (if (and k v)
      (if (= "OK" (st/set-val! k v))
        {:ok true}
        {:error "on attempt to set value"}) 
      {:error "missing key or value"})))

;;------------------------------
;; set value to exchange
;;------------------------------
(defn exch-val! [conf req]
  (let [mp-id (hu/req->mp-id req)
        m     (hu/req->value req)
        k     (hu/req->key req)]
    (exch/to! mp-id m k)))
    
(defn cmd [conf req] {(keyword (hu/req->key req)) (keyword (hu/req->value req))})

