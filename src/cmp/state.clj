(ns cmp.state
  ^{:author "wactbprot"
    :doc "Finds and starts the up comming 
          tasks of a certain container."}
  (:require [taoensso.timbre :as log]
            [cmp.st-mem :as st]
            [cmp.work :as work]            
            [cmp.task :as tsk]
            [cmp.utils :as u]))


(defn state-map->struct-key
  "Converts a `state-map` into a key."
  [m]
  (u/vec->key [(:mp-name m) (:struct m) (:no-idx m)
               "definition" (:seq-idx m) (:par-idx m)]))

(defn state-key->state-map
  "Converts a key into a `state-map`."
  [k]
  {:mp-name (st/key->mp-id k)
   :struct (st/key->struct k)
   :no-idx (st/key->no-idx k)
   :seq-idx (st/key->seq-idx k)
   :par-idx (st/key->par-idx k)
   :state (keyword (st/key->val k))})

(defn ks->state-map
  "Builds the state map `m` belonging to a key set `ks`.
  `m` is introduced in order to keep the functions testable.

  
  ```clojur
  (ks->state-map (k->state-ks \"wait@container@0\"))
  ```" 
  [ks]
  (mapv state-key->state-map ks))

(defn k->state-ks
  "Returns the state keys for a given path.

  ```clojure
  (k->state-ks \"wait@container@0\")
  ```" 
  [p]
  (sort (st/key->keys
         (u/vec->key [(st/key->mp-id p)
                      (st/key->struct p)
                      (st/key->no-idx p)
                      "state"]))))

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
       (st/key->val)
       (u/get-next-ctrl)
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

(defn all-working
  "Returns all  steps with the state
  `:working` for a given state map `m`.
  
  ```clojure
  (def m
  [{:seq-idx 0, :par-idx 0, :state :working}
   {:seq-idx 1, :par-idx 0, :state :working}
   {:seq-idx 2, :par-idx 0, :state :executed}
   {:seq-idx 3, :par-idx 0, :state :ready}
   {:seq-idx 4, :par-idx 0, :state :executed}
   {:seq-idx 5, :par-idx 0, :state :ready}])

  (all-working m)
  ```"
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
  (let [am (all-ready m)
        n (count am)]
    (cond
      (= n 0) {}
      :else (first am))))

(defn predecessor-executed?
  "Checks if `all-executed?` in the
  step `i-1` of `m`."
  [m i]
  (all-executed?
   (seq-idx->all-par m (- i 1))))

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
      (cond
        (or
         (= i 0)
         (predecessor-executed? m i)) next-m))))


;;------------------------------
;; ready!
;;------------------------------
(defn ready! 
  "Sets all states (the state interface) to ready."
  [k]
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
  (log/info "all done at " k)
  (let [ctrl-k   (k->ctrl-k k)
        cmd      (ctrl-k->cmd ctrl-k)
        state-ks (k->state-ks k)]
    (log/info "ctrl cmd is: " cmd)
    (condp = cmd
      :run (do
             (de-observe! ctrl-k)
             (st/set-val! ctrl-k "ready"))
      :mon (do
             (de-observe! ctrl-k)
             (st/set-val! ctrl-k "mon"))
      (log/info "default condp branch in all-exec fn of " k ))))

;;------------------------------
;; pick next task
;;------------------------------
(defn start-next!
  "Receives the key `k` and picks the next thing to do.
  `k` is
  * the `ctrl-key` on the first run
  * the `state-key` on the following calls
  The keys (must be a string) and look like this:

  ```clojure
  ;; ctrl-key
  (start-next! \"se3-calib@container@0@ctrl\")

  ;; state-key
  (start-next! \"se3-calib@container@0@state@1@1\")

  ```
  
  **NOTE:**

  `start-next!` only starts the first of
  `(find-next state-map)` (the upcomming tasks)
  since the workers set the state to `\"working\"`
  which triggers the next call to `start-next!`."
  [x]
  (log/info "start-next! on " x)
  (if-let [k x]
    (let [ctrl-k   (k->ctrl-k k)
          state-m  (ks->state-map (k->state-ks ctrl-k))
          next-m   (find-next state-m)]
      (log/debug "next map is: " next-m)
      (cond
        (errors?       state-m) (error!    ctrl-k)
        (all-executed? state-m) (all-exec! ctrl-k)
        (nil?           next-m) (nop!      ctrl-k)
        :run-worker  (work/dispatch
                      (state-map->struct-key next-m))))))

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
                 (fn [msg] (start-next! (st/msg->key msg))))
  (start-next! k))

;;------------------------------
;; status 
;;------------------------------
(defn cont-status
  "Return the state map for the `i`th
  container."
  [mp-id i]
  (->> (st/cont-state-path mp-id i)
       (k->state-ks)
       (ks->state-map)))

(defn defins-status
  "Return the `state-map` for the `i`th
  definition*s* structure."
  [mp-id i]
  (->> (st/defins-state-path mp-id i)
       (k->state-ks)
       (ks->state-map)))

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
