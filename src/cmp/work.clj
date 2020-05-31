(ns cmp.work
  ^{:author "wactbprot"
    :doc "Runs the upcomming tasks of a certain container."}
  (:require [taoensso.timbre :as timbre]
            [clojure.core.async :as a]
            [cmp.st-mem :as st]
            [cmp.worker.wait :refer [wait!]]
            [cmp.worker.write-exchange :refer [write-exchange!]]
            [cmp.worker.select :refer [select-definition!]]
            [cmp.worker.devhub :refer [devhub!]]
            [cmp.task :as tsk]
            [cmp.utils :as u]))

;;------------------------------
;; task 
;;------------------------------
(defn get-task
  "Returns the assembled `task` for the given key `k`
  pointing to the `proto-task`.
  Since the functions in the `cmp.task` namespace are
  (kept) independent from the tasks position, this info
  (`:StateKey` holds the position of the task)
  have to be `assoc`ed here." 
  [x]
  (if (map? x)
    x
    (let [k x]
      (try
        (if-let [proto-task (st/key->val k)]
          (let [meta-task (tsk/gen-meta-task proto-task)
                mp-id     (st/key->mp-id k)
                state-key (u/replace-key-at-level 3 k "state")]
            (tsk/assemble meta-task mp-id state-key))
          )
        (catch Exception e
          (str "error at building task at: " k
               " catch: " (.getMessage e)))))))

;;------------------------------
;; dispatch 
;;------------------------------
(defn dispatch
  "Dispatches to the workers depending on `(:Action task)`.

  Since every worker has to manage the state,
  the  `state-key` is the second parameter.

  ```clojure
  (dispatch {:Action \"wait\" :WaitTime 1000 :StateKey \"testpath\"})
  ;; #object ... ManyToManyChannel@1247ab05...
  ;; INFO [cmp.worker.wait:20] - wait time ( 1000 ms) over for  testpath
  (dispatch! {:Action \"foo\" :StateKey \"testpath\"})
  ;; ERROR [cmp.work:52] - unknown action:  :foo
  ```"  
  [x]
  (let [task (get-task x)]
    (if-let [state-key (:StateKey task)]
      (let [action (keyword (:Action task))]
        (timbre/info "cond for action: " action)
        (condp = action
          :select         (a/go (select-definition! task state-key))
          :writeExchange  (a/go (write-exchange!    task state-key))
          :wait           (a/go (wait!              task state-key))
          :MODBUS         (a/go (devhub!            task state-key))
          :TCP            (a/go (devhub!            task state-key))
          :VXI11          (a/go (devhub!            task state-key))
          :EXECUTE        (a/go (devhub!            task state-key))
          (do
            (timbre/error "unknown action: " action)
            (st/set-val! state-key "error"))))
      (timbre/debug "task has no state key: " x))))
