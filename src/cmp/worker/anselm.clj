(ns cmp.worker.anselm
  ^{:author "wactbprot"
    :doc "Worker to interact with a json api."}
  (:require [clj-http.client :as http]
            [cmp.config :as cfg]
            [cmp.resp :as resp]
            [cmp.st-mem :as st]
            [cmp.utils :as u]
            [taoensso.timbre :as log]))

(defn url
  "Builds up the url for a `anselm` request."
  [{path :RequestPath}]
  (str (cfg/anselm-url (cfg/config)) "/" path))
  
(defn req
  "Builds up the `req`est map for a `anselm` request."
  [{action :Action state-key :StateKey value :Value}]
  (assoc (cfg/json-post-header (cfg/config))
         :body
         (u/map->json value)))

(defn anselm!
  "Interacts with a json api.
  
  ```clojure
  ;; leads to a post req:
   (json-api! {:Action \"Anselm\" :Value {:DocPath \"Calibration.Measurement.Values.Position\",
                                          :Target_pressure_value 1,
                                          :Target_pressure_unit \"Pa\"}
  :RequestPath \"dut_max\"})
  ```"
  [task]
  (let [{value       :Value
         state-key   :StateKey} task]
    (st/set-state! state-key :working)
    (if (nil? value)
      (try ; get
        (resp/check (http/get (url task)) task state-key)
        (catch Exception e
          (st/set-state! state-key :error (str "get request to url: " url " failed"))))
      (try ; post
        (resp/check (http/post (url task) (req task)) task state-key)
        (catch Exception e
          (st/set-state! state-key :error "post request to url: " url " failed"))))))
