(ns cmp.worker.read-exchange
  ^{:author "wactbprot"
    :doc "Reads from exchange interface and 
          writes to docpath."}
  (:require [cmp.doc                 :as doc]
            [cmp.exchange            :as exch]
            [com.brunobonacci.mulog  :as mu]
            [cmp.st-mem              :as st]))

(defn read-exchange!
  "Reads the `exch-val` from `:ExchangePath` and writes the result to
  `:DocPath`. The `exch-val` have to be turned into a vector, to fit
  the `doc/store!` function.
  
  ```clojure
  (read-exchange! {})
  ```"
  [task]
  (let [{doc-path :DocPath exch-path :ExchangePath mp-id :MpName state-key :StateKey} task]
    (st/set-state! state-key :working)
    (let [exch-val (exch/read! mp-id exch-path)
          res-doc  (doc/store! mp-id [exch-val] doc-path)]
      (if (:ok res-doc)
        (st/set-state! state-key (if (exch/stop-if task) :executed :ready) "res doc ok")
        (st/set-state! state-key :error)))))
