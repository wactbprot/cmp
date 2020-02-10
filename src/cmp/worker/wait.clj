(ns cmp.worker.wait
  ^{:author "wactbprot"
    :doc "wait worker."}
  (:require [taoensso.timbre :as timbre]
            [clojure.core.async :as a]
            [cmp.st-mem :as st]
            [cmp.utils :as u]))

(defn wait!
  "Delays the `mp` for the time given with `:WaitTime`.
  
  ```clojure
  (wait! {:WaitTime 1000} \"testpath\")
  ```"
  [task state-key]
  (println "##########")
  (println state-key)

  (st/set-val! state-key "working")
  (a/go
    (let [w (read-string (str (task :WaitTime)))]
    (a/<! (a/timeout w))
    (timbre/info "wait time (" w "ms) over for " state-key)
    (st/set-val! state-key "executed"))))