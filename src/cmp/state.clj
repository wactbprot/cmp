(ns cmp.state
  ^{:author "wactbprot"
    :doc "Finds and starts the up comming tasks of a certain
          container."}
  (:require [taoensso.timbre :as log]
            [cmp.st-mem :as st]
            [cmp.work :as work]            
            [cmp.task :as tsk]
            [cmp.key-utils :as ku]
            [cmp.utils :as u]))

(defn state-key->state-map  
  "Builds a `state-map` by means of the `info-map`.
  The state value is `assoc`ed afet getting it with `st/key->val`. "
  [state-key]
  (assoc (ku/key->info-map state-key)
         :state (keyword (st/key->val state-key))))

(defn ks->state-vec
  "Builds the state map `m` belonging to a key set `ks`.
  `m` is introduced in order to keep the functions testable.

  Example:
  ```clojure
  (ks->state-vec (k->state-ks \"ref@container@0\"))
  ```" 
  [ks]
  (when ks
    (mapv state-key->state-map ks)))

(defn ctrl-k->cmd
  "Gets the `cmd` from the `ctrl-k`. Extracts the `next-ctrl-cmd` and
  make a keyword out of it."
  [k]
  (->> k st/key->val u/next-ctrl-cmd keyword))
  
(defn filter-state
  "Returns a vector of maps where state is `s`."
  [v s]
  (filterv (fn [m] (= s (:state m))) v))
  
(defn all-error
  "Returns all steps with the state `:error` for a given state vector
  `v`"
  [v]
  (filter-state v :error))

(defn all-ready
  "Returns all steps with the state `:ready` for a given state vector
  `v`"
  [v]
  (filter-state v :ready))

(defn all-executed
  "Returns all-executed entrys of the given vector `v`.

  Example:
  ```clojure
    (all-executed (ku/seq-idx->all-par
                      [{:seq-idx 0 :par-idx 0 :state :executed}
                       {:seq-idx 0 :par-idx 0 :state :ready}]
                     0))
  ;; =>
  ;; [{:seq-idx 0 :par-idx 0 :state :executed}]
  ```"
  [v]
  (filter-state v :executed))

(defn all-executed?
  "Checks if all entries of map `m` are executed"
  [v]
  (= (count v) (count (all-executed v))))

(defn errors?
  "Checks if there are any errors in the
  vector of maps `v`."
  [v]
  (not (empty? (all-error v))))
          
(defn next-ready
  "Returns a map (or `nil`) with the next step with state `:ready`."
  [v]
  (first (all-ready v)))

(defn predecessors-executed?
  "Checks if `all-executed?` in the steps before `i` of `v`."
  [v i]
  (let [i (u/ensure-int i)]
    (if (< 0 i)
      (every? true? (map
                     (fn [j]
                       (all-executed?
                        (ku/seq-idx->all-par v j)))
                     (range i)))
      true)))

;;------------------------------
;; ready!
;;------------------------------
(defn ready! 
  "Sets all states (the state interface) to ready."
  [k]
  (st/set-val! (ku/k->ctrl-k k) "ready")
  (st/set-same-val! (ku/k->state-ks k) "ready"))

;;------------------------------
;; stop
;;------------------------------
(defn de-observe!
  "Opposite of [[observe!]]: De-registers the `state` listener.  The
  de-register pattern is derived from the key `k` (may be the
  `ctrl-key` or `state-key`).  Resets the state interface afterwards."
  [k]
  (st/de-register! (ku/key->mp-id k) (ku/key->struct k) (ku/key->no-idx k) "state"))

;;------------------------------
;; set value at ctrl-path 
;;------------------------------
(defn error!
  "Sets the `ctrl` interface to `\"error\"`."
  [k]
  (log/error  "error! for: " k)
  (st/set-val! (ku/k->ctrl-k k) "error"))

(defn nop!
  "No operation."
  [k]
  (log/debug "nop! for: " k))

(defn all-exec!
  "Handles the case where all `state` interfaces are
  `\"executed\"`. Gets the value the `ctrl`"
  [k]
  (let [ctrl-k   (ku/k->ctrl-k k)
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
  "The `next-map` function returns a map containing the next step to
  start. See `cmp.state-test/next-map-i` for examples how `next-map`
  should work.

  Example:
  ```clojure
   (next-map [{:seq-idx 0 :par-idx 0 :state :executed}
              {:seq-idx 0 :par-idx 1 :state :executed}
              {:seq-idx 1 :par-idx 0 :state :executed}
              {:seq-idx 2 :par-idx 0 :state :executed}
              {:seq-idx 3 :par-idx 0 :state :working}
              {:seq-idx 3 :par-idx 1 :state :ready}])
  ;; =>
  ;; {:seq-idx 3 :par-idx 1 :state :ready}
  ```
  "
  [v]
  (when-let [next-m (next-ready v)]
    (when-let [i (:seq-idx next-m)]
      (when (or (zero? (u/ensure-int i))
                (predecessors-executed? v i))
        next-m))))

;;------------------------------
;; start-next(!)
;;------------------------------
(defn start-next
  "Side effect free. Makes [[start-next!]] testable.
  Gets the state vector `v` and picks the next thing to do.
  The `ctrl-k`ey is derived from the first map in the
  the `v`."
  [v]
  (let [m      (next-map v)
        ctrl-k (ku/info-map->ctrl-key (first v))
        defi-k (ku/info-map->definition-key m)]
    (cond
      (errors?       v) {:what :error    :key ctrl-k}
      (all-executed? v) {:what :all-exec :key ctrl-k}
      (nil?          m) {:what :nop      :key ctrl-k}
      :run-worker       {:what :work     :key defi-k})))

(defn start-next!
  "`start-next!` choose the `k` of the upcomming tasks.
  Then the `worker` set the state to `\"working\"` which triggers the
  next call to `start-next!`: parallel tasks are started this way.

  Side effects all around. "
  [v]
  (log/debug "call to start next")
    (when (vector? v)
      (let [{what :what k :key} (start-next v)]
        (condp = what
          :error    (error!     k)
          :all-exec (all-exec!  k)
          :nop      (nop!       k)
          :work     (work/check k)))))

;;------------------------------
;; observe!
;;------------------------------
(defn observe!
  "Registers a listener with a [[start-next!]] callback.  Calls
  `start-next!` as a first trigger.  The register pattern is derived
  from the key `k` (`ctrl-key`)."
  [k]
  (log/info "register start-next! callback and start-next!")
  (st/register! (ku/key->mp-id k) (ku/key->struct k) (ku/key->no-idx k) "state"
                (fn [msg]
                  (when-let [msg-k (st/msg->key msg)]                   
                    (log/debug "will call start-next from callback")
                    (start-next! (ks->state-vec (ku/k->state-ks msg-k))))))
  (log/debug "will call start-next first trigger")
  (start-next! (ks->state-vec (ku/k->state-ks k))))

;;------------------------------
;; status 
;;------------------------------
(defn cont-status
  "Return the `state-vec` for the `i`th container."
  [mp-id i]
  (ks->state-vec (ku/k->state-ks (st/cont-state-path mp-id i))))

(defn defins-status
  "Return the `state-vec` for the `i`th definition*s* structure."
  [mp-id i]
  (ks->state-vec (ku/k->state-ks (st/defins-state-path mp-id i))))

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
