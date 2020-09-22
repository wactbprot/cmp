(ns cmp.worker.json-api
  ^{:author "wactbprot"
    :doc "Worker to interact with a json api."}
  (:require [clj-http.client :as http]
            [cmp.config :as cfg]
            [cmp.resp :as resp]
            [cmp.st-mem :as st]
            [cmp.utils :as u]
            [taoensso.timbre :as log]))

(defn json-api!
  "Interacts with a json api.
 
  ```clojure
  ;; leads to a post req:
   (json-api! {:Action \"Anselm\" :Value {:DocPath \"Calibration.Measurement.Values.Position\",
                                          :Target_pressure_value 1,
                                          :Target_pressure_unit \"Pa\"}
  :RequestPath \"dut_max\"})
  ```"
  [task]
  (let [{value :Value
         state-key :StateKey
         path :RequestPath} task]
    (st/set-state! state-key :working)
    (let [url (str (cfg/json-api-url (cfg/config)) "/" path)]
      (prn url)
      (if (nil? value)
        (try
          (resp/check (http/get url)  task state-key)
          (catch Exception e
            (st/set-state! state-key :error)
            (log/error "get request failed"))))
      (let [req (assoc (cfg/json-api-post-header (cfg/config))
                       :body
                       (u/map->json value))]
        (try
          (resp/check (http/post url req)  task state-key)
          (catch Exception e
            (st/set-state! state-key :error)
            (log/error "post request failed")))))))

