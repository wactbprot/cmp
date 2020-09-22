(ns cmp.worker.read-exchange
  ^{:author "wactbprot"
    :doc "Reads from exchange interface and 
          writes to docpath."}
  (:require [taoensso.timbre :as log]
            [cmp.st-mem :as st]
            [cmp.doc :as doc]
            [cmp.exchange :as exch]))

(defn read-exchange!
  "Reads the `exch-val` from `:ExchangePath` and writes the result
  to `:DocPath`. The `exch-val` have to be turned into a vector, to fit
  the `doc/store!` function.
  
  ```clojure
  (read-exchange! {})
  ```"
  [{doc-path :DocPath exch-path :ExchangePath mp-id :MpName state-key :StateKey}]
  (st/set-state! state-key :working)
  (let [exch-val [(exch/read! mp-id exch-path)]
        res-doc  (doc/store! mp-id exch-val doc-path)]
    (cond
      (:error res-doc) (st/set-state! state-key :error)
      (:ok res-doc)    (st/set-state! state-key :executed)
      :unexpected      (st/set-state! state-key :error))))