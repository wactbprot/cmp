(ns cmp.worker.wait
  ^{:author "wactbprot"
    :doc "wait worker."}
  (:require [taoensso.timbre :as log]
            [cmp.st-mem      :as st]
            [cmp.exchange    :as exch]
            [cmp.config      :as cfg]))

(def mtp (cfg/min-task-period (cfg/config)))
(defn wait!
  "Delays the `mp` for the time given with `:WaitTime`.
  
  ```clojure
  (wait! {:WaitTime 1000})
  ```"
  [task]
  (let [{wait-time :WaitTime
         state-key :StateKey} task]
    (st/set-state! state-key :working)
    (let [w (read-string (str wait-time))]
      (if (< w mtp)
        (Thread/sleep mtp)
        (Thread/sleep w))
      (st/set-state! state-key (if (exch/stop-if task) :executed :ready) "wait time over"))))
