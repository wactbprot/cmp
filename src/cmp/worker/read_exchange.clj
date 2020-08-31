(ns cmp.worker.read-exchange
  ^{:author "wactbprot"
    :doc "Reads from exchange interface and 
          writes to docpath."}
  (:require [taoensso.timbre :as log]
            [cmp.st-mem :as st]
            [cmp.doc :as doc]
            [cmp.exchange :as exch]))

(defn read-exchange!
  "Reads from `:ExchangePath` and writes the result
  to `:DocPath`.
  
  ```clojure
  (read-exchange! {})
  ```"
  [{doc-path :DocPath exch-path :ExchangePath mp-id :MpName state-key :StateKey}]
  (st/set-state! state-key :working)
  (let [exch-val (exch/read! mp-id exch-path)
        ;;res-doc  (doc/store! mp-id exch-val doc-path)
        ]
    (prn exch-val)
    (comment (cond
      (:error res-doc) (st/set-state! state-key :error)
      (:ok res-doc)    (st/set-state! state-key :executed)
      :unexpected      (st/set-state! state-key :error)))))

