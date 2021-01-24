(ns cmp.worker.write-exchange
  ^{:author "wactbprot"
    :doc "wait worker."}
  (:require [cmp.config              :as cfg]
            [cmp.exchange            :as exch]
            [com.brunobonacci.mulog  :as mu]
            [cmp.st-mem              :as st]))

(defn write-exchange!
  "Writes the `:Value` to the exchange.
  
  ```clojure
  (write-exchange! {:Value {:a 1 :b 2} :MpName \"ref\"})
  ;;
  (st/key->val \"ref@exchange@b\")
  ;; 2
  ```"
  [task]
  (let [{val :Value mp-id :MpName state-key :StateKey exch-path :ExchangePath} task]
  (st/set-state! state-key :working)
  (let [ret (exch/to! mp-id val exch-path)]
    (if (:ok ret)
      (st/set-state! state-key (if (exch/stop-if task) :executed :ready) "wrote to exchange")
      (st/set-state! state-key :error "error on attempt to write exchange")))))