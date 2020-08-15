(ns cmp.worker.write-exchange
  ^{:author "wactbprot"
    :doc "wait worker."}
  (:require [taoensso.timbre :as log]
            [cmp.st-mem :as st]
            [cmp.exchange :as exch]
            [cmp.config :as cfg]))

(defn write-exchange!
  "Writes the `:Value` to the exchange.
  
  ```clojure
  (write-exchange! {:Value {:a 1 :b 2} :MpName \"ref\"})
  ;;
  (st/key->val \"ref@exchange@b\")
  ;; 2
  ```"
  [{val :Value mp-id :MpName state-key :StateKey}]
  (st/set-state! state-key :working)
  (let [res (exch/to! mp-id val)]
    (cond
      (:error res) (do
                     (st/set-state! state-key :error)
                     (log/error "error on attempt to write exchange"))
      (:ok res)    (do 
                     (st/set-state! state-key :executed)
                     (log/info "wrote to exchange"))
      :default     (do
                     (st/set-state! state-key :error)
                     (log/warn "unclear exchange response")))))
