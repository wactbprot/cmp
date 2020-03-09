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
  (wait! {:WaitTime 1000} \"testpath\")
  ```"
  [task state-key]
  (st/set-val! state-key "working")
  (let [w (read-string (str (task :WaitTime)))]
    (if (< w mtp)
      (Thread/sleep mtp)
      (Thread/sleep w))
    (timbre/info "wait time (" w "ms) over for " state-key)
    (st/set-val! state-key "executed")))
