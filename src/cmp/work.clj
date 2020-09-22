(ns cmp.work
  ^{:author "wactbprot"
    :doc "Runs the upcomming tasks of a certain container."}
  (:require [taoensso.timbre :as log]
            [cmp.st-mem :as st]
            [cmp.exchange :as exch]
            [cmp.worker.json-api :refer [json-api!]]
            [cmp.worker.wait :refer [wait!]]
            [cmp.worker.run-mp :refer [run-mp!]]
            [cmp.worker.read-exchange :refer [read-exchange!]]
            [cmp.worker.write-exchange :refer [write-exchange!]]
            [cmp.worker.select :refer [select-definition!]]
            [cmp.worker.devhub :refer [devhub!]]
            [cmp.task :as tsk]
            [cmp.config :as cfg]
            [cmp.utils :as u]))

(def mtp (cfg/min-task-period (cfg/config)))

;;------------------------------
;; task 
;;------------------------------
(defn get-task
  "Returns the assembled `task` for the given key `k` related to the
  `proto-task`.  Since the functions in the `cmp.task` namespace are
  (kept) independent from the tasks position, this info (`:StateKey`
  holds the position of the  task) have to be `assoc`ed
  (done in `tsk/assemble`)." 
  [k]
    (try
      (if-let [proto-task (st/key->val k)]
        (let [meta-task (tsk/gen-meta-task proto-task)
              mp-id     (st/key->mp-id k)
              state-key (u/replace-key-at-level 3 k "state")]
          (tsk/assemble meta-task mp-id state-key)))
      (catch Exception e
          (str "error at building task at: " k
               " catch: " (.getMessage e)))))

;;------------------------------
;;  future registry 
;;------------------------------
(defonce future-reg (atom {}))

(defn start!
  "Starts the worker in a new threat. This means that all workers
  may be single threated."
  [worker task]
  (swap! future-reg assoc
         (:StateKey task) (future (worker task))))

;;------------------------------
;;  dispatch 
;;------------------------------
(defn dispatch
  "Dispatch depending on the `:Action`."
  [task]
  (condp = (keyword (:Action task))
    :select         (start! select-definition! task)
    :runMp          (start! run-mp!            task)
    :writeExchange  (start! write-exchange!    task)
    :readExchange   (start! read-exchange!     task)
    :wait           (start! wait!              task)
    :Anselm         (start! json-api!          task)
    :MODBUS         (start! devhub!            task)
    :TCP            (start! devhub!            task)
    :VXI11          (start! devhub!            task)
    :EXECUTE        (start! devhub!            task)
    (st/set-state! (:StateKey task) :error (str "No worker for action: " (:Action task)))))

;;------------------------------
;; check
;;------------------------------
(defn check
  "Gets the task. Handles the `:RunIf` and `:StopIf`
  cases.

  ```clojure
  (dispatch {:Action \"wait\" :WaitTime 1000 :StateKey \"testpath\"})
  ```"  
  [x]
  (let [task     (if (string? x) (get-task  x) x)
        state-key (:StateKey task)
        run-if    (:RunIf    task)
        stop-if   (:StopIf   task)]
    (if (nil? run-if)
      (if (nil? stop-if)
        (dispatch task) ;; no run-if or stop-if
        (if (exch/stop-if task)
          (st/set-state! state-key :executed "state set by stop-if") 
          (dispatch task))) ;; don't stop
      (if (exch/run-if task)
        (dispatch task) ;; run
        (st/set-state! state-key :ready "state set by run-if")))))
