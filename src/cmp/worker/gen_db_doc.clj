(ns cmp.worker.gen-db-doc
  ^{:author "wactbprot"
    :doc "Worker to create database documents."}
  (:require [clj-http.client :as http]
            [cmp.config      :as cfg]
            [cmp.doc         :as d]
            [cmp.resp        :as resp]
            [cmp.st-mem      :as st]
            [cmp.lt-mem      :as lt]
            [cmp.utils       :as u]
            [taoensso.timbre :as log]))


(defn gen-db-doc!
  "Generates a couchdb document from the value.

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
         doc       :Value
         mp-id     :MpName} task]
    (st/set-state! state-key :working)
    (let [doc-id (:_id doc)
          url    (str (cfg/lt-conn (cfg/config)) "/" doc-id) 
          req    (assoc (cfg/json-post-header (cfg/config))
                        :body
                        (u/map->json (lt/rev-refresh doc)))]
      (try
        (resp/check (http/put url req) task state-key)
        (d/add mp-id doc-id)
        (catch Exception e
          (st/set-state! state-key :error)
          (log/error "put request to url: " url "failed")
          (log/error  e))))))
