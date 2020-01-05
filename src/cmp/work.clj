(ns cmp.work
  ^{:author "wactbprot"
    :doc "Runs the upcomming tasks of a certain container."}
  (:require [taoensso.timbre :as timbre]
            [clojure.core.async :as a]
            [cmp.st-mem :as st]
            [cmp.worker.wait :refer [wait!]]
            [cmp.worker.select :refer [select-definition!]]
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
;; ctrl channel invoked by run 
;;------------------------------
(def ctrl-chan (a/chan))

(defn k->task
  "Returns the assembled task for the given key" 
  [k]
  (assoc 
   (->> k
        (st/key->val)
        (u/json->map)
        (tsk/gen-meta-task)
        (tsk/assemble))
   :StructKey k
   :StateKey (u/replace-key-at-level 3 k "state")))

;;------------------------------
;; dispatch 
;;------------------------------
(defn dispatch!
  "Dispatches to the workers depending on `:Action`.
  Since every worker have to set their state, `state-key`
  is the second parameter."  
  [task]
  (let [state-key (task :StateKey)
        action (task :Action)]
    (cond
      (= action "wait") (wait! task state-key)
      (= action "select") (select-definition! task state-key)
      :default (do
                 (timbre/error "unknown action: " action)
                 (st/set-val! state-key "error")))))

;;------------------------------
;; ctrl go block 
;;------------------------------
(a/go
  (while true  
    (let [k (a/<! ctrl-chan)]
      (try
        (timbre/debug "receive key " k
                      " try to get task and call worker")            
        (dispatch! (k->task k))
        (catch Exception e
          (timbre/error "catch error at channel " k)
          (a/>! excep-chan e))))))
