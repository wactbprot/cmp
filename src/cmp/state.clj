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
  "Converts a key into a `state-map`."
  [k]
  {:mp-name (st/key->mp-id k)
   :struct (st/key->struct k)
   :no-idx (st/key->no-idx k)
   :seq-idx (st/key->seq-idx k)
   :par-idx (st/key->par-idx k)
   :state (keyword (st/key->val k))})

(defn ks->state-vec
  "Builds the state map `m` belonging to a key set `ks`.
  `m` is introduced in order to keep the functions testable.

  
  ```clojur
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
  [m s]
  (filter (fn [x] (= s (:state x))) m))

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
  [m i]
  (filter (fn [x] (= i (:seq-idx x))) m))
  
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
  [m]
  (=
   (count m)
   (count (all-executed m))))

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
  step `i-1` (`(dec i)`) of `m`."
  [m i]
  (all-executed?
   (seq-idx->all-par m (dec i))))

(defn find-next
  "The `find-next` function
  returns a list of maps containing the next
  tasks to start. It should work as follows:

  ```clojure
  cmp.run>   (def m
  [{:seq-idx 0, :par-idx 0, :state :ready}
   {:seq-idx 0, :par-idx 1, :state :ready}
   {:seq-idx 1, :par-idx 0, :state :ready}
   {:seq-idx 2, :par-idx 0, :state :ready}
   {:seq-idx 3, :par-idx 0, :state :ready}
   {:seq-idx 3, :par-idx 0, :state :ready}])
  
  ;;  #'cmp.run/m
  cmp.run> (find-next m)
  ;;  {:seq-idx 0, :par-idx 0, :state :ready}
  cmp.run>   (def m
  [{:seq-idx 0, :par-idx 0, :state :working}
   {:seq-idx 0, :par-idx 1, :state :ready}
   {:seq-idx 1, :par-idx 0, :state :ready}
   {:seq-idx 2, :par-idx 0, :state :ready}
   {:seq-idx 3, :par-idx 0, :state :ready}
   {:seq-idx 3, :par-idx 0, :state :ready}])
  
  ;;  #'cmp.run/m
  cmp.run> (find-next m)
  ;;  {:seq-idx 0, :par-idx 1, :state :ready}
  cmp.run>   (def m
  [{:seq-idx 0, :par-idx 0, :state :working}
   {:seq-idx 0, :par-idx 1, :state :working}
   {:seq-idx 1, :par-idx 0, :state :ready}
   {:seq-idx 2, :par-idx 0, :state :ready}
   {:seq-idx 3, :par-idx 0, :state :ready}
   {:seq-idx 3, :par-idx 0, :state :ready}])
  
  ;;  #'cmp.run/m
  cmp.run> (find-next m)
  ;;  nil
  cmp.run>   (def m
  [{:seq-idx 0, :par-idx 0, :state :executed}
   {:seq-idx 0, :par-idx 1, :state :executed}
   {:seq-idx 1, :par-idx 0, :state :ready}
   {:seq-idx 2, :par-idx 0, :state :ready}
   {:seq-idx 3, :par-idx 0, :state :ready}
   {:seq-idx 3, :par-idx 0, :state :ready}])
  
  ;;  #'cmp.run/m
  cmp.run> (find-next m)
  ;;  {:seq-idx 1, :par-idx 0, :state :ready}
  ```
  It should not crash on:

  ```clojure
  cmp.run> (count (next-ready m))
  ;; 0
  cmp.run> (find-next m)
  ;; nil
  cmp.run> (find-next {})
  ;; nil
  cmp.run> (find-next nil)
  ;; nil
  ```"
  [m]
  (when-let [next-m (next-ready m)]
    (when-let [i (next-m :seq-idx)]
      (when (or
             (zero? i)
             (predecessor-executed? m i))
        next-m))))

;;------------------------------
;; ready!
;;------------------------------
(defn ready! 
  "Sets all states (the state interface) to ready."
  [k]
  (st/set-same-val! (k->state-ks k) "ready")
  (st/set-val! (k->ctrl-k k) "ready"))

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
                   "state")
  (ready! k))

;;------------------------------
;; suspend!
;;------------------------------
(defn suspend!
  "Simply de-registers the `state` listener
  without changing (e.g. ready!) the state interface.
  The de-register pattern is derived
  from the key  `k` (may be the
  `ctrl-key` or `state-key`)."
  [k]
  (let [state-ks (k->state-ks k)]
    (st/de-register! (st/key->mp-id k)
                     (st/key->struct k)
                     (st/key->no-idx k)
                     "state")))

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
      :run (do
             (de-observe! ctrl-k)
             (st/set-val! ctrl-k "ready"))
      :mon (do
             (de-observe! ctrl-k)
             (st/set-val! ctrl-k "mon"))
      (log/info "default condp branch in all-exec fn of " k ))))

;;------------------------------
;; choose and start next task
;;------------------------------
(defn choose-next
  "Receives the `state-vec` and picks the next thing to do
  without side effects.
  
  **NOTE:**
  
  `choose-next!` only choose the first of
  `(find-next state-map)` (the upcomming tasks)
  since the workers set the state to `\"working\"`
  which triggers the next call to `start-next!`."
 
  [state-vec]
  (when (vector? state-vec)
    (let [next-m (find-next state-vec)
          ctrl-k (state-map->ctrl-key (first state-vec))
          defi-k (state-map->definition-key next-m)]
      (cond
        (errors?       state-vec) {:what :error    :k ctrl-k}
        (all-executed? state-vec) {:what :all-exec :k ctrl-k}
        (nil?          next-m)    {:what :nop      :k ctrl-k}
        :run-worker               {:what :work     :k defi-k}))))

(defn start-next!
  "Side effects all around. "
  [state-vec]
  (when (vector? state-vec)
    (let [{what :what
           k    :k} (choose-next state-vec)]
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
                   (start-next! (ks->state-vec
                                 (k->state-ks
                                  (st/msg->key msg))))))
  (start-next! (ks->state-vec
                (k->state-ks k))))

;;------------------------------
;; status 
;;------------------------------
(defn cont-status
  "Return the state map for the `i`th
  container."
  [mp-id i]
  (ks->state-vec (k->state-ks (st/cont-state-path mp-id i))))

(defn defins-status
  "Return the `state-map` for the `i`th
  definition*s* structure."
  [mp-id i]
  (->> (st/defins-state-path mp-id i)
       (k->state-ks)
       (ks->state-vec)))

;;------------------------------
;; dispatch
;;------------------------------
(defn dispatch
  "Dispaches depending on `cmd`."
  [k cmd]
  (condp = (keyword cmd)
    :run     (observe!    k)
    :mon     (observe!    k)
    :stop    (de-observe! k)
    :reset   (ready!      k)
    :suspend (suspend!    k)
    (log/info  "default case state dispach function" )))
