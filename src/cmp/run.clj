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

(defn par-run
  [ks]
  (run!
   (fn [k]
     (let [recipe-path (u/replace-key-at-level 3 k "definition")
           proto-task (u/gen-map (st/get-val recipe-path))
           meta-task (tsk/gen-meta-task proto-task)
           task (tsk/assemble meta-task)]
       (println (assoc task
                       :Mp (u/key->mp-name k)
                       :Struct (u/key->struct k)
                       :No (u/key->no-idx k)
                       :Seq (u/key->seq-idx k)
                       :Par (u/key->par-idx k)))))
   ks))

(defn ks->state-map
  [ks]
  (mapv
   (fn [k]
     {:seq-idx (u/key->seq-idx k)
      :par-idx (u/key->par-idx k)
      :state (st/get-val k)})
   ks))

(defn p->state-ks
  [p]
  (sort (st/get-keys
         (u/replace-key-at-level 3 p "state"))))

(defn p->state-map
  [p]
  (let [ks (p->state-ks p)]
    (ks->state-map ks)))

(defn filter-state
  [v s]
  (filter (fn [m] (= s (m :state))) v))

(defn filter-par
  [v n]
  (filter (fn [m] (= n (m :par-idx))) v))
  
(defn all-error
  [v]
  (filter-state v "error"))

(defn all-ready
  [v]
  (filter-state v "ready"))

(defn all-working
  [v]
  (filter-state v "working"))

(defn all-executed
  [v]
  (filter-state v "executed"))

(defn all-executed?
  [v]
  (=
   (count v)
   (count (all-executed v))))

(defn errors?
  [v]
  (not (empty? (all-error v))))

(defn all-ready?
  [v]
  (=
   (count v)
   (count (all-ready v))))

(defn par-step-complete?
  [v n]
  (let [n-all (count (filter-par v n))
        n-exec (count (filter-par (all-executed v) n))]
    (and
     (= n-all n-exec)
     (> n-exec 0))))
          
(defn next-ready
  [v]
  (first all-ready))

(defn choose
  [p]
  (let [state-map (p->state-map p)]
    (cond
      (errors?  state-map) (println "got errors")
      (all-ready? state-map) (println "all ready")
      (all-executed? state-map) (println "all executed"))))
    
;;------------------------------
;; worker 
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
        (choose p)
        (catch Exception e
          (timbre/error "catch error at channel " p)
          (a/>! excep-chan e))))))
