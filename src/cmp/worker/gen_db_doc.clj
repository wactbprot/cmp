(ns cmp.worker.gen-db-doc
  ^{:author "wactbprot"
    :doc "Worker to replicate a couchdb database."}
  (:require [clj-http.client :as http]
            [cmp.config :as cfg]
            [cmp.resp :as resp]
            [cmp.st-mem :as st]
            [cmp.lt-mem :as lt]
            [cmp.utils :as u]
            [taoensso.timbre :as log]))


(defn gen-db-doc!
  "Generates a couchdb database document from the value.

  ```clojure
  (t-assemble \"SE3_state-gen_state_doc\")
  ;; {:Action \"genDbDoc\",
  ;; :Comment \"generates a state doc for storing results\",
  ;; :TaskName \"SE3_state-gen_state_doc\",
  ;; :Value
  ;; {:_id \"state-se3_20200923\",
  ;; :State
  ;; {:Measurement
  ;;  {:Date [{:Type \"generated\", :Value \"2020-09-23 10:37:28\"}],
  ;;   :AuxValues {},
  ;;   :Values {}}}},
  ;; :MpName \"core\",
  ;; :StateKey \"core@test@0@state@0@0\"}
  ```
  "
  [task]
  (let [{state-key :StateKey
         doc       :Value} task]
    (st/set-state! state-key :working)
    (let [url (str (cfg/lt-conn (cfg/config)) "/" (:_id doc)) 
          req (assoc (cfg/json-post-header (cfg/config))
                     :body
                     (u/map->json (lt/rev-refresh doc)))]
      (try
        (resp/check (http/put url req) task state-key)
        (catch Exception e
          (st/set-state! state-key :error)
          (log/error "put request to url: " url "failed")
          (log/error  e))))))
