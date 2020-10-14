(ns cmp.work
  ^{:author "wactbprot"
    :doc "Runs the upcomming tasks of a certain container."}
  (:require [taoensso.timbre :as log]
            [cmp.st-mem :as st]
            [cmp.exchange :as exch]
            [cmp.task :as tsk]
            [cmp.config :as cfg]
            [cmp.utils :as u]
            [cmp.worker.anselm         :refer [anselm!]]
            [cmp.worker.devhub         :refer [devhub!]]
            [cmp.worker.gen-db-doc     :refer [gen-db-doc!]]
            [cmp.worker.get-date       :refer [get-date!]]
            [cmp.worker.get-time       :refer [get-time!]]
            [cmp.worker.message        :refer [message!]]
            [cmp.worker.read-exchange  :refer [read-exchange!]]
            [cmp.worker.replicate-db   :refer [replicate!]]
            [cmp.worker.run-mp         :refer [run-mp!]]
            [cmp.worker.select         :refer [select-definition!]]
            [cmp.worker.wait           :refer [wait!]]
            [cmp.worker.write-exchange :refer [write-exchange!]]
            ))

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
  (let [state-key (:StateKey task)]
    (swap! future-reg assoc
           state-key (future (worker task)))
    (log/debug (str "registered worker @work/future-reg at key: " state-key))))

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
    :getDate        (start! get-date!          task)
    :getTime        (start! get-time!          task)
    :message        (start! message!           task)
    :genDbDoc       (start! gen-db-doc!        task)
    :replicateDB    (start! replicate!         task)
    :Anselm         (start! anselm!            task)
    :MODBUS         (start! devhub!            task)
    :TCP            (start! devhub!            task)
    :VXI11          (start! devhub!            task)
    :EXECUTE        (start! devhub!            task)
    (st/set-state! (:StateKey task) :error (str "No worker for action: " (:Action task)))))

;;------------------------------
;; check
;;------------------------------
(defn check-in
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


;;------------------------------
;; check-out
;;------------------------------
(defn check-out
  "Function is used by the workers to set state.
  An optional log message may be provided."
  ([k state msg]
   (condp = state
     :error (log/error msg)
     :ready (log/info msg)
     (log/debug msg))
   (set-state! k state))
  ([k state]
   (when (and (string? k)
              (keyword? state))
     (Thread/sleep mtp)
     (set-val! k (name state))
     (log/debug "wrote new state: " state " to: " k))))
