(ns cmp.worker.devproxy
  ^{:author "wactbprot"
    :doc "Worker to interact with a json api."}
  (:require [cmp.config             :as cfg]
            [clj-http.client        :as http]
            [com.brunobonacci.mulog :as mu]
            [cmp.resp               :as resp]
            [cmp.st-mem             :as st]
            [cmp.st-utils           :as stu]
            [cmp.utils              :as u]))

(defn url
  "Builds up the url for a `anselm` request."
  [{path :RequestPath}]
  (str (cfg/dev-proxy-url (cfg/config)) "/" path))
  
(defn req
  "Builds up the `req`est map for a `devproxy` request."
  [{value :Value}]
  (assoc (cfg/json-post-header (cfg/config))
         :body
         (u/map->json value)))

(defn devproxy!
  "Interacts with a json api.
  
  ```clojure
  ;; leads to a post req:
   (devproxy! {:Value {:DocPath \"Calibration.Measurement.Values.Position\",
                       :Target_pressure_value 1,
                       :Target_pressure_unit \"Pa\"}
  :RequestPath \"dut_max\"})
  ```"
  [{value :Value state-key :StateKey :as task}]
  (let [request-key (stu/key->request-key state-key)]
    (st/set-state! state-key :working)
    (st/set-val! request-key task)
    (mu/log ::anselm! :message "stored request, send request" :key request-key)
    (if-not value
      (try ; get
        (resp/check (http/get (url task)) task state-key)
        (catch Exception e
          (st/set-state! state-key :error "get request failed")))
      (try ; post
        (resp/check (http/post (url task) (req task)) task state-key)
        (catch Exception e
          (st/set-state! state-key :error "post request to failed"))))))
