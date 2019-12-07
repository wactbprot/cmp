(ns cmp.work
  ^{:author "wactbprot"
    :doc "Runs the upcomming tasks of a certain container."}
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
;; ctrl channel invoked by run 
;;------------------------------
(def ctrl-chan (a/chan))


(defn k->task
  [k]
  (let [proto-task (u/gen-map (st/get-val k))
        meta-task (tsk/gen-meta-task proto-task)]
        (tsk/assemble meta-task)))

(defn assoc-dyn-info
  "Enriches the task with runtime infos"
  [task k]
  (assoc task
         :mp-id (u/key->mp-name k)
         :struct (u/key->struct k)
         :no-idx (u/key->no-idx k)
         :seq-idx (u/key->seq-idx k)
         :par-idx (u/key->par-idx k)))

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
;; ctrl go block 
;;------------------------------
(a/go
  (while true  
    (let [k (a/<! ctrl-chan)]
      (println ".....................")
      (println k)
      (println ".....................")
      (try
        (println (k->task k) )
        (catch Exception e
          (timbre/error "catch error at channel " k)
          (a/>! excep-chan e))))))
