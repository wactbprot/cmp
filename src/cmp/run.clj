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
  "Returns the state-map for a given path.

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

(defn filter-par
  [m i]
  (filter (fn [x] (= i (x :par-idx))) m))
  
(defn all-error
  [v]
  (filter-state v :error))

(defn all-ready
  [m]
  (filter-state m :ready))

(defn all-working
  [m]
  (filter-state m :working))

(defn all-executed
  "Returns all-executed entrys of the given state-map.
  ```clojure
  (def m
  [{:seq-idx 0, :par-idx 0, :state :ready}
   {:seq-idx 1, :par-idx 0, :state :ready}
   {:seq-idx 2, :par-idx 0, :state :ready}
   {:seq-idx 3, :par-idx 0, :state :ready}
   {:seq-idx 4, :par-idx 0, :state :ready}
   {:seq-idx 5, :par-idx 0, :state :ready}])

  (all-executed m)
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

(defn par-step-complete?
  [m i]
  (let [n-all (count (filter-par m i))
        n-exec (count (filter-par (all-executed m) i))]
    (and
     (= n-all n-exec)
     (> n-exec 0))))
          
(defn next-ready
  [m]
  (first (all-ready m)))

(defn pick-next
  "Receives the path p and picks the next thing to do.
  p looks like this (must be a string):
  ```
  se3-calib@container@0@ctrl
  ```"
  [p]
  (let [state-map (p->state-map p)]
    (cond
      (errors?  state-map) (println "got errors")
      (all-executed? state-map) (println "all executed"))))
    
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
