(ns cmp.run
  ^{:author "wactbprot"
    :doc "Runs the upcomming tasks of a certain container."}
  (:require [taoensso.timbre :as timbre]
            [clojure.core.async :as a]
            [cmp.st :as st]
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

(defn k->task
  [k]
  (let [recipe-path (u/replace-key-at-level 3 k "definition")
        proto-task (u/gen-map (st/get-val recipe-path))
        meta-task (tsk/gen-meta-task proto-task)]
        (tsk/assemble meta-task)))

(defn assoc-dyn-info
  [task k]
  (assoc task
         :Mp (u/key->mp-name k)
         :Struct (u/key->struct k)
         :No (u/key->no-idx k)
         :Seq (u/key->seq-idx k)
         :Par (u/key->par-idx k)))

  (defn ks->state-map
  [ks]
  (mapv
   (fn [k]
     {:seq-idx (u/key->seq-idx k)
      :par-idx (u/key->par-idx k)
      :state (keyword (st/get-val k))})
   ks))

(defn p->state-ks
  [p]
  (sort (st/get-keys
         (u/replace-key-at-level 3 p "state"))))

(defn p->state-map
  "Returns the state-map for a given path
  (path must be string).

  Example (see also [[pick-next]]):
  ```clojure
  (p->state-map se3-calib@container@0@ctrl)
  ```"
  [p]
  (let [ks (p->state-ks p)]
    (ks->state-map ks)))

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
      (errors?  state-map) (println "got errors")
      (all-executed? state-map) (println "all executed")
      :else next-to-start)))

;;------------------------------
;; demo worker 
;;------------------------------
(defmulti worker
  (fn [task] (task :Action)))

(defmethod worker :Wait
  [task]
  (println "wait"))

(defmethod worker :default
  [task]
  (println (task :Action)))

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
