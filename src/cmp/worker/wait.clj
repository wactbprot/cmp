(ns cmp.worker.wait
  ^{:author "wactbprot"
    :doc "wait worker."}
  (:require [taoensso.timbre :as timbre]
            [cmp.st-mem :as st]
            [cmp.config :as cfg]))

(def mtp (cfg/min-task-period (cfg/config)))

(defn wait!
  "Delays the `mp` for the time given with `:WaitTime`.
  
  ```clojure
  (wait! {:WaitTime 1000 :StateKey \"test\"})
  ```"
  [{wait-time :WaitTime state-key :StateKey}]
  
  (st/set-val! state-key "working")
  (timbre/debug "start with wait, already set " state-key  " working")

  (let [w (read-string (str wait-time))]
    (if (< w mtp)
      (Thread/sleep mtp)
      (Thread/sleep w))
    (timbre/info "wait time (" w "ms) over for " state-key)
    (st/set-val! state-key "executed")
    {:ok true}))
