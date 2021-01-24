(ns cmp.worker.wait
  ^{:author "wactbprot"
    :doc "wait worker."}
  (:require [com.brunobonacci.mulog  :as mu]
            [cmp.st-mem              :as st]
            [cmp.utils               :as u]))


(defn wait!
  "Delays the `mp` for the time given with `:WaitTime`.

  Example:
  ```clojure
  (wait! {:WaitTime 1000})
  ```"
  [{wait-time :WaitTime state-key :StateKey}]
  (st/set-state! state-key :working)
  (Thread/sleep (u/ensure-int wait-time))
  (st/set-state! state-key :executed "wait time over"))
