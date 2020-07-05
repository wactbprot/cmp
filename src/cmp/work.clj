(ns cmp.work
  ^{:author "wactbprot"
    :doc "Runs the upcomming tasks of a certain container."}
  (:require [taoensso.timbre :as log]
            [clojure.core.async :as a]
            [cmp.st-mem :as st]
            [cmp.exchange :as exch]
            [cmp.worker.wait :refer [wait!]]
            [cmp.worker.run-mp :refer [run-mp!]]
            [cmp.worker.write-exchange :refer [write-exchange!]]
            [cmp.worker.select :refer [select-definition!]]
            [cmp.worker.devhub :refer [devhub!]]
            [cmp.task :as tsk]
            [cmp.utils :as u]))

;;------------------------------
;; task 
;;------------------------------
(defn get-task
  "Returns the assembled `task` for the given key `x` related to the
  `proto-task` or returns `x` if not a struct key.  Since the
  functions in the `cmp.task` namespace are (kept) independent from
  the tasks position, this info (`:StateKey` holds the position of the
  task) have to be `assoc`ed in `tsk/assemble`." 
  [x]
  (if (string? x)
    (try
      (if-let [proto-task (st/key->val x)]
        (let [meta-task (tsk/gen-meta-task proto-task)
              mp-id     (st/key->mp-id x)
              state-key (u/replace-key-at-level 3 x "state")]
          (tsk/assemble meta-task mp-id state-key)))
      (catch Exception e
          (str "error at building task at: " x
               " catch: " (.getMessage e))))
    ;; x is already a task:
    x))

;;------------------------------
;;  dispatch 
;;------------------------------
(defn dispatch
  [task]
  (let [state-key (:StateKey task)
        action (keyword (:Action task))]
    (condp = action
      :select         (a/go (select-definition! task))
      :runMp          (a/go (run-mp!            task))
      :writeExchange  (a/go (write-exchange!    task))
      :wait           (a/go (wait!              task))
      :MODBUS         (a/go (devhub!            task))
      :TCP            (a/go (devhub!            task))
      :VXI11          (a/go (devhub!            task))
      :EXECUTE        (a/go (devhub!            task))
      (when state-key
        (log/error "unknown action in task:" task)
        (st/set-val! state-key "error")))))

;;------------------------------
;; check
;;------------------------------
(defn check
  "Gets the task. Handles the `:RunIf` and `:StopIf`.

  ```clojure
  (dispatch {:Action \"wait\" :WaitTime 1000 :StateKey \"testpath\"})
  ```"  
  [x]
  (let [task      (get-task  x)
        state-key (:StateKey task)
        run-if    (:RunIf    task)
        stop-if   (:StopIf   task)]
    (if (nil? run-if)
      (if (nil? stop-if)
        (dispatch task) ;; no run-if or stop-if
        (if (exch/stop-if task)
          (st/set-val! state-key "executed") ;; stop
          (dispatch task))) ;; don't stop
      (if (exch/run-if task)
        (dispatch task) ;; exec
        (st/set-val! state-key "ready") ;; don't exec but trigger re-eval
        ))))
