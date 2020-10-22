(ns cmp.state
  ^{:author "wactbprot"
    :doc "Finds and starts the up comming 
          tasks of a certain container."}
  (:require [taoensso.timbre :as log]
            [cmp.st-mem :as st]
            [cmp.work :as work]            
            [cmp.task :as tsk]
            [cmp.utils :as u]))


(defn state-map->definition-key
  "Converts a `state-map` into the belonging `definition` key."
  [m]
  (when (map? m)
    (u/vec->key [(:mp-name m) (:struct m) (:no-idx m) "definition"
                 (:seq-idx m) (:par-idx m)])))

(defn state-map->ctrl-key
  "Converts a `state-map` into the belonging `ctrl` key."
  [m]
  (when (map? m)
    (u/vec->key [(:mp-name m) (:struct m) (:no-idx m) "ctrl"])))

(defn state-key->state-map
  "Builds a `state-map` by means of the key structure
  and `st/key->val`. "
  [k]
  {:mp-name (st/key->mp-id        k)
   :struct  (st/key->struct       k)
   :no-idx  (st/key->no-idx       k)
   :seq-idx (st/key->seq-idx      k)
   :par-idx (st/key->par-idx      k)
   :state   (keyword (st/key->val k))})

(defn ks->state-vec
  "Builds the state map `m` belonging to a key set `ks`.
  `m` is introduced in order to keep the functions testable.

  
  ```clojure
  (ks->state-vec (k->state-ks \"wait@container@0\"))
  ```" 
  [ks]
  (when ks
    (mapv state-key->state-map ks)))

(defn k->state-ks
  "Returns the state keys for a given path.

  ```clojure
  (k->state-ks \"wait@container@0\")
  ```" 
  [k]
  (when k
    (sort
     (st/key->keys
      (u/vec->key [(st/key->mp-id k)
                   (st/key->struct k)
                   (st/key->no-idx k)
                   "state"])))))

(defn k->ctrl-k
  "Returns the `ctrl`-key for a given key `k`.
  In other words: ensures k to be a `ctrl-key`.

  ```clojure
  (k->ctrl-k \"wait@container@0@state@0@0\")
  ;; \"wait@container@0@ctrl\"
  (k->ctrl-k
    (k->ctrl-k
      (k->ctrl-k \"wait@container@0@state@0@0\")))
    ;; \"wait@container@0@ctrl\"
  ```" 
  [k]
  (u/vec->key [(st/key->mp-id k)
               (st/key->struct k)
               (st/key->no-idx k)
               "ctrl"]))

(defn ctrl-k->cmd
  "Gets the `cmd` from the `ctrl-k`."
  [k]
  (->> k
       st/key->val
       u/next-ctrl-cmd
       keyword))
  
(defn filter-state
  [v s]
  (filterv (fn [m] (= s (:state m))) v))

(defn seq-idx->all-par
  "Returns all `par` steps for a given
  state map `m` and `seq-idx`

  ```clojure
  (def m
  [{:seq-idx 0, :par-idx 0, :state :executed}
   {:seq-idx 1, :par-idx 0, :state :ready}
   {:seq-idx 2, :par-idx 0, :state :executed}
   {:seq-idx 4, :par-idx 0, :state :ready}
   {:seq-idx 4, :par-idx 1, :state :executed}
   {:seq-idx 4, :par-idx 2, :state :ready}])
  
  (seq-idx->all-par m 4)
  ({:seq-idx 4, :par-idx 0, :state :ready}
  {:seq-idx 4, :par-idx 1, :state :executed}
  {:seq-idx 4, :par-idx 2, :state :ready})
  ```"
  [v i]
  (filterv (fn [m] (= (u/ensure-int i)
                     (u/ensure-int (:seq-idx m)))) v))
  
(defn all-error
  "Returns all  steps with the state
  `:error` for a given state map `m`"
  [m]
  (filter-state m :error))

(defn all-ready
  "Returns all  steps with the state
  `:ready` for a given state map `m`"
  [m]
  (filter-state m :ready))

(defn all-working
  "Returns all  steps with the state
  `:working` for a given state map `m`"
  [m]
  (filter-state m :working))

(defn all-executed
  "Returns all-executed entrys of the given `state-map`.

  ```clojure
  (def m
  [{:seq-idx 0, :par-idx 0, :state :ready}
   {:seq-idx 1, :par-idx 0, :state :ready}
   {:seq-idx 2, :par-idx 0, :state :executed}
   {:seq-idx 3, :par-idx 0, :state :ready}
   {:seq-idx 4, :par-idx 0, :state :executed}
   {:seq-idx 4, :par-idx 0, :state :ready}])

  (all-executed (seq-idx->all-par m 4))
  ;; gives
  ;; ({:seq-idx 4, :par-idx 0, :state :executed})

  ```"
  [m]
  (filter-state m :executed))

(defn all-executed?
  "Checks if all entries of map `m`
  are executed"
  [v]
  (= (count v) (count (all-executed v))))

(defn errors?
  "Checks if there are any errors in the map `m`."
  [m]
  (not (empty? (all-error m))))
          
(defn next-ready
  "Returns a map with the next
  step with state `:ready`. Returns an
  empty map if nothing next"
  [m]
  (let [am (all-ready m)]
    (if (zero? (count am))
      {}
      (first am))))

(defn predecessor-executed?
  "Checks if `all-executed?` in the
  steps before  `i` of `v`."
  [v i]
  (let [int-i (u/ensure-int i)]
    (if (< 0 int-i)
      (every? true? (map
                     (fn [j]
                       (all-executed?
                        (seq-idx->all-par v j)))
                     (range int-i)))
      true)))

;;------------------------------
;; ready!
;;------------------------------
(defn ready! 
  "Sets all states (the state interface) to ready."
  [k]
  (st/set-val! (k->ctrl-k k) "ready")
  (st/set-same-val! (k->state-ks k) "ready"))

;;------------------------------
;; stop
;;------------------------------
(defn de-observe!
  "Opposite of [[observe!]]:
  De-registers the `state` listener.
  The de-register pattern is derived
  from the key  `k` (may be the
  `ctrl-key` or `state-key`).
  Resets the state interface afterwards."
  [k]
  (st/de-register! (st/key->mp-id k)
                   (st/key->struct k)
                   (st/key->no-idx k)
                   "state"))

;;------------------------------
;; set value at ctrl-path 
;;------------------------------
(defn error!
  "Sets the `ctrl` interface to `\"error\"`."
  [k]
  (log/error  "error! for: " k)
  (st/set-val! (k->ctrl-k k) "error"))

(defn nop!
  "No operation."
  [k]
  (log/debug "nop! for: " k))

(defn all-exec!
  "Handles the case where all `state` interfaces
  are `\"executed\"`. Gets the value  the `ctrl`"
  [k]
  (let [ctrl-k   (k->ctrl-k k)
        cmd      (ctrl-k->cmd ctrl-k)]
    (log/info "all done at: " k "ctrl interface cmd is: " cmd)
    (condp = cmd
      :mon   (do
               (de-observe! ctrl-k)
               (ready! ctrl-k)
               (st/set-val! ctrl-k "mon"))
      (do (de-observe! ctrl-k)
          (ready! ctrl-k)
          (st/set-val! ctrl-k "ready")
          (log/info "default condp branch in all-exec fn of " k )))))

;;------------------------------
;; choose and start next task
;;------------------------------

(defn next-map
  "The `next-map` function returns a map containing the next
  step to start. See `cmp.state-test/next-map-i` for examples
  how  `next-map` should work.
  
  ```clojure
   (next-map [{:seq-idx 0, :par-idx 0, :state :executed}
              {:seq-idx 0, :par-idx 1, :state :executed}
              {:seq-idx 1, :par-idx 0, :state :executed}
              {:seq-idx 2, :par-idx 0, :state :executed}
              {:seq-idx 3, :par-idx 0, :state :working}
              {:seq-idx 3, :par-idx 1, :state :ready}])
  ;; cmp.state> {:seq-idx 3, :par-idx 1, :state :ready}
  ```
  "
  [v]
  (when-let [next-m (next-ready v)]
    (when-let [i (:seq-idx next-m)]
      (when (or
             (zero? (u/ensure-int i))
             (predecessor-executed? v i))
        next-m))))

(defn choose-next
  "Gets the state vector `v` and picks the next thing to do.
  The `ctrl-k`ey is derived from the first map in the
  the `v`."
  [v]
  (when (vector? v)
    (let [m      (next-map v)
          ctrl-k (state-map->ctrl-key (first v))
          defi-k (state-map->definition-key m)]
      (cond
        (errors?       v) {:what :error    :k ctrl-k}
        (all-executed? v) {:what :all-exec :k ctrl-k}
        (nil?          m) {:what :nop      :k ctrl-k}
        :run-worker       {:what :work     :k defi-k}))))

(defn start-next!
  "`start-next!` choose the `k` of the upcomming tasks.
  Then the `worker` set the state to `\"working\"`
  which triggers the next call to `start-next!`:
  parallel tasks are started this way.

  Side effects all around. "
  [v]
  (log/debug "call to start next")
    (when (vector? v)
      (let [{what :what
             k    :k} (choose-next v)]
        (condp = what
          :error    (error! k)
          :all-exec (all-exec! k)
          :nop      (nop! k)
          :work     (work/check k)))))
      
;;------------------------------
;; observe!
;;------------------------------
(defn observe!
  "Registers a listener with a [[start-next!]] callback.
  Calls `start-next!` as a first trigger.
  The register pattern is derived
  from the key  `k` (`ctrl-key`)."
  [k]
  (log/info "register start-next! callback and start-next!")
  (st/register!  (st/key->mp-id k)
                 (st/key->struct k)
                 (st/key->no-idx k)
                 "state"
                 (fn [msg]
                   (when-let [msg-k (st/msg->key msg)]                   
                     (log/debug "will call start next from callback")
                     (start-next! (ks->state-vec
                                   (k->state-ks
                                    msg-k))))))
  (log/debug "will call start first trigger")
  (start-next! (ks->state-vec
                (k->state-ks k))))

;;------------------------------
;; status 
;;------------------------------
(defn cont-status
  "Return the `state-vec` for the `i`th
  container."
  [mp-id i]
  (ks->state-vec (k->state-ks (st/cont-state-path mp-id i))))

(defn defins-status
  "Return the `state-vec` for the `i`th
  definition*s* structure."
  [mp-id i]
  (ks->state-vec (k->state-ks (st/defins-state-path mp-id i))))

;;------------------------------
;; dispatch
;;------------------------------
(defn dispatch
  "Dispaches depending on `cmd`."
  [k cmd]
  (condp = (keyword cmd)
    :run     (observe! k)
    :mon     (observe! k)
    :stop    (do
               (de-observe! k)
               (ready! k))
    :reset   (do
               (de-observe! k)
               (ready! k))
    :suspend (de-observe! k)
    (log/info  "default case state dispach function" )))
