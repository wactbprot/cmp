(ns cmp.worker.get-date
  ^{:author "wactbprot"
    :doc "Worker to create a date entry in documents."}
  (:require [clj-http.client :as http]
            [cmp.config :as cfg]
            [cmp.doc :as doc]
            [cmp.st-mem :as st]
            [cmp.utils :as u]
            [taoensso.timbre :as log]))


(defn get-date!
  "Generates a Date object and stores it under the path
  `DocPath`.

  ```clojure
  (t-assemble \"Common-get_date\")
  ;; {:TaskName \"Common-get_date\",
  ;; :Action \"getDate\",
  ;; :Comment \"Saves a date string (path: )\",
  ;; :DocPath \"Calibration.Measurement.Date\",
  ;; :Type \"%type\",
  ;; :MpName \"core\",
  ;; :StateKey \"core@test@0@state@0@0\"}
  ```
  "
  [task]
  (let [{type      :Type
         doc-path  :DocPath
         state-key :StateKey
         mp-id     :MpName}  task]
    (st/set-state! state-key :working)
    (let [res [{:Type type :Value (u/get-date)}]
          ret (doc/store! mp-id res doc-path)]
      (cond
        (:error ret) (do
                       (log/error "failed to write time stamp")
                       (log/error ret)
                       (st/set-state! state-key :error))
        (:ok    ret) (st/set-state! state-key :executed)
        :unexpected  (do
                       (log/error "unexpected return value")
                       (log/error ret)
                       (st/set-state! state-key :error))))))
