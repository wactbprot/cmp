(ns cmp.state
  ^{:author "wactbprot"
    :doc "Finds and starts the up comming 
          tasks of a certain container."}
  (:require [taoensso.timbre :as timbre]
            [clojure.core.async :as a]
            [cmp.st-mem :as st]
            [cmp.work :as work]            
            [cmp.task :as tsk]
            [cmp.utils :as u]))
;;------------------------------
;; exception channel 
;;------------------------------
(def excep-chan (a/chan))
(a/go
  (while true
    (let [e (a/<! excep-chan)] 
      (timbre/error (.getMessage e)))))

;;------------------------------
;; ctrl channel invoked by ctrl 
;;------------------------------
(def ctrl-chan (a/chan))

(defn state-map->definition-key
  "Converts a `state-map` into a key."
  [m]
  (u/vec->key [(:mp-name m) (:struct m) (:no-idx m)
               "definition" (:seq-idx m) (:par-idx m)]))

(defn state-key->state-map
  "Converts a key into a `state-map`."
  [k]
  {:mp-name (u/key->mp-name k)
   :struct (u/key->struct k)
   :no-idx (u/key->no-idx k)
   :seq-idx (u/key->seq-idx k)
   :par-idx (u/key->par-idx k)
   :state (keyword (st/key->val k))})

(defn ks->state-map
  "Builds the state map `m` belonging to a key set `ks`.
  `m` is introduced in order to keep the functions testable.

  
  ```clojure
  (ks->state-map (p->state-ks \"wait@container@0\"))
  ```" 
  [ks]
  (mapv state-key->state-map ks))

(defn p->state-ks
  "Returns the state keys for a given path.

  ```clojure
  (p->state-ks \"wait@container@0\")
  ```" 
  [p]
  (sort (st/get-keys
         (u/vec->key [(u/key->mp-name p)
                      (u/key->struct p)
                      (u/key->no-idx p)
                      "state"]))))

(defn p->ctrl-k
  "Returns the `ctrl`-key for a given path `p`.

  ```clojure
  (p->ctrl-k \"wait@container@0@state@0@0\")
  ```" 
  [p]
  (u/vec->key [(u/key->mp-name p)
               (u/key->struct p)
               (u/key->no-idx p)
               "ctrl"]))

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
         (predecessor-executed? m i)) next-m
        :else nil))))

;;------------------------------
;; set value at ctrl-path 
;;------------------------------
(defn error-ctrl!
  "Sets the `ctrl` interface to `\"error`."
  [p]
  (st/set-val! (p->ctrl-k p) "error"))

(defn all-exec-ctrl!
  "Handles the case where all `state` interfaces
  are `\"executed\"`."
  [p]
  (let [ctrl-k   (p->ctrl-k p)
        ctrl-str (->> ctrl-k
                      (st/key->val)
                      (u/get-next-ctrl))
        state-ks (p->state-ks p)]
    (cond
      (= ctrl-str "run") (do
                           (timbre/info "all done at " p " (run branch)")
                           (st/set-val! ctrl-k "ready")
                           (st/set-same-val! state-ks "ready")
                           (st/de-register! (u/key->mp-name p)
                                             (u/key->struct p)
                                             (u/key->no-idx p)
                                             "state"))
      (= ctrl-str "mon") (do
                           (timbre/info "all done at " p " (run mon)")
                           (st/set-same-val! state-ks "ready")
                           (st/set-val! ctrl-k "mon")))))

(defn nil-ctrl!
  "Kind of `nop`."
  [p])

;;------------------------------
;; pick next task
;;------------------------------
(defn start-next!
  "Receives the path p and picks the next thing to do.
  `p` looks like this (must be a string):

  ```clojure
  (start-next! \"se3-calib@container@0@ctrl\")
  ```
  
  **NOTE:**

  `start-next` only starts the first of
  `(find-next state-map)` (the upcomming tasks)
  since the workers set the state to `\"working\"`
  which triggers the next call to `start-next`."
  [p]
  (let [ctrl-p  (p->ctrl-k p)
        state-m (ks->state-map (p->state-ks ctrl-p))
        next-m  (find-next state-m)]
    (cond
      (errors?       state-m) (error-ctrl!    ctrl-p)
      (all-executed? state-m) (all-exec-ctrl! ctrl-p)
      (nil?          next-m)  (nil-ctrl!      ctrl-p)
      :else (a/go
              (a/<! (a/timeout 200))
              (a/>! work/ctrl-chan (state-map->definition-key next-m))))))

;;------------------------------
;; start, stop
;;------------------------------
(defn start
  "Registers a listener with a [[start-next!]] callback.
  Calls `start-next!` as a first trigger."
  [p]
  (st/register! (u/key->mp-name p)
                 (u/key->struct p)
                 (u/key->no-idx p)
                 "state"
                 (fn [msg] (start-next! (st/msg->key msg))))
  (start-next! p))

(defn stop
  "De-registers the state listener."
  [p]
  (st/de-register! (u/key->mp-name p)
                    (u/key->struct p)
                    (u/key->no-idx p)
                    "state"))

;;------------------------------
;; status 
;;------------------------------
(defn status
  "Return the state map for the `n`th
  container."
  [mp-id n]
  (->> [mp-id "container" n "state"]
       (u/vec->key)
       (p->state-ks)
       (ks->state-map)))

;;------------------------------
;; ctrl go block 
;;------------------------------
(a/go
  (while true  
    (let [[k cmd] (a/<! ctrl-chan)]
      (try
        (timbre/debug "receive key " k "and" cmd)            
        (condp = cmd
          :start (start k)
          :stop (stop k))
        (catch Exception e
          (timbre/error "catch error at channel " k)
          (a/>! excep-chan e))))))
