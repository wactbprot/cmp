(ns cmp.worker.write-exchange
  ^{:author "wactbprot"
    :doc "wait worker."}
  (:require [taoensso.timbre :as timbre]
            [cmp.st-mem :as st]
            [cmp.exchange :as exch]
            [cmp.config :as cfg]))

(def mtp (cfg/min-task-period (cfg/config)))

(defn write-exchange!
  "Writes the `:Value` to the exchange.
  
  ```clojure
  (wait! {:WaitTime 1000} \"testpath\")
  ```"
  [task state-key]
  (st/set-val! state-key "working")
  (Thread/sleep mtp)
  (let [mp-id (:MpName task)
        m     (:Value task)
        res   (exch/to! mp-id m)]
    (cond
      (:error res) (do
                     (prn "error")
                     (st/set-val! state-key "error"))
      (:ok res)    (do
                     (prn "ok")
                     (st/set-val! state-key "executed"))
      :default (prn "should not happen"))))
