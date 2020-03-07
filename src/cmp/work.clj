(ns cmp.work
  ^{:author "wactbprot"
    :doc "Runs the upcomming tasks of a certain container."}
  (:require [taoensso.timbre :as timbre]
            [clojure.core.async :as a]
            [cmp.st-mem :as st]
            [cmp.worker.wait :refer [wait!]]
            [cmp.worker.select :refer [select-definition!]]
            [cmp.worker.modbus :refer [modbus!]]
            [cmp.excep :as excep]
            [cmp.task :as tsk]
            [cmp.utils :as u]))

;;------------------------------
;; ctrl channel invoked by run 
;;------------------------------
(def ctrl-chan (a/chan))

(defn k->task
  "Returns the assembled `task` for the given key `k`
  pointing to the `proto-task`.
  Since the function in the `cmp.task` namespace are
  kept independent from the tasks position, runtime infos
  like `:StructKey` have to be `assoc`ed here" 
  [k]
  (if-let [proto-task (st/key->val k)]
    (tsk/assemble
     (assoc  (tsk/gen-meta-task proto-task)
             :StructKey k
             :MpName    (u/key->mp-name k)
             :StateKey  (u/replace-key-at-level 3 k "state")))
    (a/>! excep/ch (throw (Exception. (str "No task at: " k))))))

;;------------------------------
;; dispatch 
;;------------------------------
(defn dispatch!
  "Dispatches to the workers depending on `:Action`.
  Since every worker has to manage the state,
  the  `state-key` is the second parameter.

  ```clojure
  (dispatch! {:Action \"wait\" :WaitTime 1000 :StateKey \"testpath\"})
  ;; #object ... ManyToManyChannel@1247ab05...
  ;; ... INFO [cmp.worker.wait:20] - wait time ( 1000 ms) over for  testpath
  (dispatch! {:Action \"foo\" :StateKey \"testpath\"})
  ;; ... ERROR [cmp.work:52] - unknown action:  :foo
  ```"  
  [task]
  (let [state-key (task :StateKey)
        action    (keyword (task :Action))]
    (condp = action
      :wait   (wait!              task state-key)
      :select (select-definition! task state-key)
      :MODBUS (modbus!            task state-key)
      (do
        (timbre/error "unknown action: " action)
        (st/set-val! state-key "error")))))

;;------------------------------
;; ctrl go block 
;;------------------------------
(a/go-loop []
  (let [k (a/<! ctrl-chan)]
    (try
      (timbre/debug "receive key " k
                    " try to get task and call worker")            
      (dispatch! (k->task k))
      (catch Exception e
        (timbre/error "catch error at channel " k)
        (a/>! excep/ch e))))
  (recur))
