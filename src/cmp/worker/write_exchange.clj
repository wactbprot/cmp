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
  (write-exchange! {:Value {:a 1 :b 2} :MpName \"ref\"})
  ;;
  (st/key->val \"ref@exchange@b\")
  ;; 2
  ```"
  [{val :Value mp-id :MpName state-key :StateKey}]
  (when state-key
    (st/set-val! state-key "working"))
  (Thread/sleep mtp)
  (let [res (exch/to! mp-id val)]
    (cond
      (:error res) (do
                     (when state-key
                       (Thread/sleep mtp)
                       (st/set-val! state-key "error"))
                     (timbre/error "error on attempt to write exchange"))
      (:ok res)    (do 
                     (when state-key
                       (Thread/sleep mtp)
                       (st/set-val! state-key "executed"))
                     (timbre/info "wrote to exchange"))
      :default     (do
                     (when state-key
                       (Thread/sleep mtp)
                       (st/set-val! state-key "error"))
                     (timbre/warn "unclear exchange response")))))
