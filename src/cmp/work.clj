(ns cmp.work
  ^{:author "wactbprot"
    :doc "Runs the upcomming tasks of a certain container."}
  (:require [com.brunobonacci.mulog    :as mu]
            [cmp.st-mem                :as st]
            [cmp.exchange              :as exch]
            [cmp.task                  :as tsk]
            [cmp.config                :as cfg]
            [cmp.utils                 :as u]
            [cmp.worker.anselm         :refer [anselm!]]
            [cmp.worker.devhub         :refer [devhub!]]
            [cmp.worker.gen-db-doc     :refer [gen-db-doc!]]
            [cmp.worker.get-date       :refer [get-date!]]
            [cmp.worker.get-time       :refer [get-time!]]
            [cmp.worker.message        :refer [message!]]
            [cmp.worker.read-exchange  :refer [read-exchange!]]
            [cmp.worker.replicate-db   :refer [replicate!]]
            [cmp.worker.ctrl-mp        :refer [run-mp! stop-mp!]]
            [cmp.worker.select         :refer [select-definition!]]
            [cmp.worker.wait           :refer [wait!]]
            [cmp.worker.write-exchange :refer [write-exchange!]]))

;;------------------------------
;;  future registry 
;;------------------------------
(defonce future-reg (atom {}))
(defn start!
  "Starts the worker in a new threat. This means that all workers
  may be single threated."
  [worker task]
  (let [state-key (:StateKey task)]
    (mu/log ::start! :message "registered worker" :key state-key)
    (swap! future-reg assoc
           state-key (future (worker task)))))

;;------------------------------
;;  dispatch 
;;------------------------------
(defn dispatch
  "Dispatch depending on the `:Action`."
  [task]
  (let [action    (keyword (:Action task))
        state-key (:StateKey task)]
    (mu/log ::dispatch :message "dispatch task" :key state-key)
    (condp = action
      :select         (start! select-definition! task)
      :runMp          (start! run-mp!            task)
      :stopMp         (start! stop-mp!           task)
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
      (st/set-state! state-key :error "No worker for action"))))

;;------------------------------
;; check-in
;;------------------------------
(defn check
  "Gets the `task` from the key `k` and calls the `dispach` function on
  it. Handles the `:RunIf` case. The `:StopIf` case is handeled by the
  `workers` after processing the task."  
  [k]
  (let [task (tsk/build k)]
    (if (exch/run-if task)
      (if (exch/only-if-not task)
        (dispatch task)
        (do
          (Thread/sleep (cfg/stop-if-delay (cfg/config)))
          (st/set-state! (:StateKey task) :executed "state set by only-if-not")))
      (do
        (Thread/sleep (cfg/stop-if-delay (cfg/config)))
        (st/set-state! (:StateKey task) :ready "state set by run-if")))))
