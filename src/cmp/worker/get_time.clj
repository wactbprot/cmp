(ns cmp.worker.get-time
  ^{:author "wactbprot"
    :doc "Worker to create a time stamp entry in documents."}
  (:require [clj-http.client :as http]
            [cmp.config      :as cfg]
            [cmp.exchange    :as exch]
            [cmp.doc         :as doc]
            [cmp.st-mem      :as st]
            [cmp.utils       :as u]
            [taoensso.timbre :as log]))

(defn get-time!
  "Generates a Date object and stores it under the path
  `DocPath`.

  ```clojure
  (t-assemble \"Common-get_time\")
  ;; {:TaskName \"Common-get_time\",
  ;; :Action \"getTime\",
  ;; :Comment \"Saves a timestamp (path: ).\",
  ;; :DocPath \"\",
  ;; :ExchangePath \"Time\",
  ;; :Type \"%type\",
  ;; :MpName \"core\",
  ;; :StateKey \"core@test@0@state@0@0\"}

  ```
  "
  [task]
  (let [{type      :Type
         doc-path  :DocPath
         exch-path :ExchangePath
         state-key :StateKey
         mp-id     :MpName}  task
        fb-exch-path "TimeStamp"]
    (st/set-state! state-key :working)
    (let [val     {:Type type :Value (u/get-time)}
          ret-doc  (doc/store! mp-id [val] doc-path)
          ret-exch (exch/to! mp-id val (if exch-path exch-path fb-exch-path))]
      (cond
        (or
         (:error ret-doc)
         (:error ret-exch)) (st/set-state! state-key :error (str "failed to write time stamp"
                                                                 ret-doc ret-exch))
        (and
         (:ok ret-doc)
         (:ok ret-exch))    (st/set-state! state-key (if (exch/stop-if task) :executed :ready) "get time executed")
        :unexpected         (st/set-state! state-key :error "unexpected return value")))))
