(ns cmp.worker.get-time
  ^{:author "wactbprot"
    :doc "Worker to create a time stamp entry in documents."}
  (:require [cmp.doc                 :as doc]
            [cmp.exchange            :as exch]
            [com.brunobonacci.mulog  :as mu]
            [cmp.st-mem              :as st]
            [cmp.utils               :as u]))

(defn get-time!
  "Generates a timestamp object `{:Type <type> :Value (u/get-time)}` and
  stores it under `:DocPath`."
  [{type :Type doc-path :DocPath exch-path :ExchangePath state-key :StateKey mp-id :MpName :as task}]
  (st/set-state! state-key :working)
  (let [val  {:Type type :Value (u/get-time)}
        ret  (doc/store! mp-id [val] doc-path)]
    (when exch-path (exch/to! mp-id val exch-path))
    (if (:ok ret)
      (st/set-state! state-key  :executed  "get time executed")
      (st/set-state! state-key :error "unexpected return value"))))
