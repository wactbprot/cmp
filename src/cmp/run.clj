(ns cmp.run
  ^{:author "wactbprot"
    :doc "Finds and starts the upcomming tasks of a certain container."}
  (:require [taoensso.timbre :as timbre]
            [clojure.core.async :as a]
            [cmp.st :as st]
            [cmp.work :as work]            
            [cmp.task :as tsk]
            [cmp.utils :as u])
  (:gen-class))

;;------------------------------
;; exception channel 
;;------------------------------
(def excep-chan (a/chan))
(a/go
  (while true
    (let [e (a/<! excep-chan)] 
      (timbre/error (.getMessage e)))))

;;------------------------------
;; ctrl channel invoked by poll 
;;------------------------------
(def ctrl-chan (a/chan))

(defn state-map->k
  "Converts a state-map into a key."
  [m]
  (u/vec->key [(m :mp-name)
               (m :struct)
               (m :no-idx)
               "definition"
               (m :seq-idx)
               (m :par-idx)]))

(defn k->state-map
  "Converts a key in a state-map."
  [k]
  {:mp-name (u/key->mp-name k)
   :struct (u/key->struct k)
   :no-idx (u/key->no-idx k)
   :seq-idx (u/key->seq-idx k)
   :par-idx (u/key->par-idx k)
   :state (keyword (st/get-val k))})

(defn ks->state-map
  "Builds the state map `m` belonging to a key set `ks`.
  `m` is introduced in order to keep the functions testable." 
  [ks]
  (mapv
   (fn [k]
     (k->state-map k))
   ks))

(defn p->state-ks
  "Returns the state keys for a given path"
  [p]
  (sort (st/get-keys
         (u/replace-key-at-level 3 p "state"))))

(defn p->state-map
  "Returns the state-map for a given path
  (path must be string).

  Example (see also [[pick-next]]):
  ```clojure
  (p->state-map se3-calib@container@0@ctrl)

  ;; gives:
  [{:mp-name se3-calib,
  :struct container,
  :seq-idx 0,
  :par-idx 0,
  :state :ready}
 {:mp-name se3-calib,
  :struct container,
  :seq-idx 1,
  :par-idx 0,
  :state :ready}]

  ```
  The `mp-name` and `struct` key is needed for reconstructing
  the path only.
  "
  [p]
  (ks->state-map
   (p->state-ks p)))

(defn filter-state
  [m s]
  (filter (fn [x] (= s (x :state))) m))

(defn seq-idx->all-par-idx
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
  
  (seq-idx->all-par-idx m 4)
  ({:seq-idx 4, :par-idx 0, :state :ready}
  {:seq-idx 4, :par-idx 1, :state :executed}
  {:seq-idx 4, :par-idx 2, :state :ready})
  ```"
  [m i]
  (filter (fn [x] (= i (x :seq-idx))) m))
  
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
  "Returns all-executed entrys of the given state-map.
  ```clojure
  (def m
  [{:seq-idx 0, :par-idx 0, :state :ready}
   {:seq-idx 1, :par-idx 0, :state :ready}
   {:seq-idx 2, :par-idx 0, :state :executed}
   {:seq-idx 3, :par-idx 0, :state :ready}
   {:seq-idx 4, :par-idx 0, :state :executed}
   {:seq-idx 4, :par-idx 0, :state :ready}])

  (all-executed (seq-idx->all-par-idx m 4))
  ;; gives
  ;; ({:seq-idx 4, :par-idx 0, :state :executed})

  ```"
  [m]
  (filter-state m :executed))

(defn all-executed?
  [m]
  (=
   (count m)
   (count (all-executed m))))

(defn errors?
  [m]
  (not (empty? (all-error m))))
          
(defn next-ready
  "Returns a map with the next
  step with state `:ready`."
  [m]
  (first (all-ready m)))

(defn predecessor-executed?
  [m i]
  (let [j (- i 1)]
    (all-executed?
     (seq-idx->all-par-idx m j))))

(defn find-next
  "The `find-next` function should work as follows:

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
  ```"
  [m]
  (let [next-m (next-ready m)
        seq-idx (next-m :seq-idx)]
    (cond
      (= seq-idx 0) next-m
      (predecessor-executed? m seq-idx) next-m
      :else nil)))

(defn pick-next
  "Receives the path p and picks the next thing to do.
  p looks like this (must be a string):
  ```
  se3-calib@container@0@ctrl
  ```"
  [p]
  (let [state-map (p->state-map p)
        next-to-start (find-next state-map)]
    (cond
      (errors? state-map) (println "got errors")
      (all-executed? state-map) (println "all executed")
      (nil? next-to-start) (println "nothing todo")
      :else (let [k (state-map->k next-to-start)]
              (a/>!! work/ctrl-chan k)))))

;;------------------------------
;; status 
;;------------------------------
(defn status
  [p]
  (p->state-map p))

;;------------------------------
;; ctrl go block 
;;------------------------------
(a/go
  (while true  
    (let [p (a/<! ctrl-chan)] 
      (try
        (pick-next p)
        (catch Exception e
          (timbre/error "catch error at channel " p)
          (a/>! excep-chan e))))))
