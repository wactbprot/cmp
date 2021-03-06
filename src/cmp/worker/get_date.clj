(ns cmp.worker.get-date
  ^{:author "wactbprot"
    :doc "Worker to create a date entry in documents."}
  (:require [cmp.config              :as cfg]
            [cmp.doc                 :as doc]
            [cmp.exchange            :as exch]
            [com.brunobonacci.mulog  :as mu]
            [cmp.st-mem              :as st]
            [cmp.utils               :as u]))

(defn get-date!
  "Generates this date object: `[{:Type <type> :Value (u/get-date)}]`
  and stores it under  `DocPath`.

  ```clojure
  (t-assemble \"Common-get_date\")
  ;; {:TaskName \"Common-get_date\",
  ;; :Action \"getDate\",
  ;; :DocPath \"Calibration.Measurement.Date\",
  ;; :Type \"%type\",
  ;; :MpName \"core\",
  ;; :StateKey \"core@test@0@state@0@0\"}
  ```"
  [{type :Type doc-path :DocPath state-key :StateKey mp-id :MpName :as task}]
  (st/set-state! state-key :working)
  (let [ret (doc/store! mp-id [{:Type type :Value (u/get-date)}] doc-path)]
    (if (:ok ret)
      (st/set-state! state-key :executed  "get date executed")
      (st/set-state! state-key :error "failed to write date"))))
