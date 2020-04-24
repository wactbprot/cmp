(ns cmp.work
  ^{:author "wactbprot"
    :doc "Runs the upcomming tasks of a certain container."}
  (:require [taoensso.timbre :as timbre]
            [clojure.core.async :as a]
            [cmp.st-mem :as st]
            [cmp.worker.wait :refer [wait!]]
            [cmp.worker.select :refer [select-definition!]]
            [cmp.worker.devhub :refer [devhub!]]
            [cmp.excep :as excep]
            [cmp.task :as tsk]
            [cmp.utils :as u]))

;;------------------------------
;; task 
;;------------------------------
(defn k->task
  "Returns the assembled `task` for the given key `k`
  pointing to the `proto-task`.
  Since the functions in the `cmp.task` namespace are
  (kept) independent from the tasks position, runtime infos
  like `:StructKey` have to be `assoc`ed here." 
  [k]
  (if-let [proto-task (st/key->val k)]
    (tsk/assemble
     (assoc (tsk/gen-meta-task proto-task)
            :StructKey k
            :MpName    (st/key->key-space k)
            :StateKey  (u/replace-key-at-level 3 k "state")))
    (a/>!! excep/ch (throw (Exception. (str "No task at: " k))))))

;;------------------------------
;; dispatch 
;;------------------------------
(defn dispatch!
  "Dispatches to the workers depending on `(:Action task)`.

  Since every worker has to manage the state,
  the  `state-key` is the second parameter.

  ```clojure
  (dispatch! {:Action \"wait\" :WaitTime 1000 :StateKey \"testpath\"})
  ;; #object ... ManyToManyChannel@1247ab05...
  ;; INFO [cmp.worker.wait:20] - wait time ( 1000 ms) over for  testpath
  (dispatch! {:Action \"foo\" :StateKey \"testpath\"})
  ;; ERROR [cmp.work:52] - unknown action:  :foo
  ```"  
  [task state-key]
  (let [action    (keyword (:Action task))]
    (timbre/info "cond for action: " action)
    (condp = action
      :wait    (wait!              task state-key)
      :select  (select-definition! task state-key)
      :MODBUS  (devhub!            task state-key)
      :TCP     (devhub!            task state-key)
      :VXI11   (devhub!            task state-key)
      :EXECUTE (devhub!            task state-key)
      (do
        (timbre/error "unknown action: " action)
        (st/set-val! state-key "error")))))


;;------------------------------
;; ctrl channel invoked by run 
;;------------------------------
(def ctrl-chan (a/chan (a/buffer 10)))

;;------------------------------
;; ctrl go block 
;;------------------------------
(a/go-loop []
  (let [k (a/<! ctrl-chan)]
    (timbre/info "receive request for: " k)
    (if-let [task (k->task k)]
      (if-let [state-key (:StateKey task)]
        (let [state (st/key->val state-key)] 
          (if (= state "ready")
            (do
              (timbre/debug "try to call worker for: " k)
              (try
                (dispatch! task state-key)
                (catch Exception e
                  (timbre/error "catch error on task dispatch for: " k)
                  (st/set-val! state-key "error")
                  (a/>! excep/ch e))))
            (timbre/debug "state is not ready for: " k)))
        (timbre/debug "task has no state key: " k))
      (timbre/debug "no task at: " k)))
  (recur))
