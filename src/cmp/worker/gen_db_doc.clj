(ns cmp.worker.gen-db-doc
  ^{:author "wactbprot"
    :doc "Worker to create database documents."}
  (:require [cmp.config              :as cfg]
            [cmp.doc                 :as d]
            [clj-http.client         :as http]
            [cmp.lt-mem              :as lt]
            [com.brunobonacci.mulog  :as mu]
            [cmp.resp                :as resp]
            [cmp.st-mem              :as st]
            [cmp.utils               :as u]))

(defn gen-url [id] (str (cfg/lt-conn (cfg/config)) "/" id))

(defn gen-req
  "Assoc a json version of the doc (with updated revision) as `:body`"
  [doc]
  (assoc (cfg/json-post-header (cfg/config))
         :body
         (u/map->json (lt/rev-refresh doc))))

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
  ```"
  [task]
  (let [{state-key :StateKey
         doc       :Value
         mp-id     :MpName} task]
    (st/set-state! state-key :working)
    (let [doc-id (:_id doc)
          url    (gen-url doc-id)
          req    (gen-req doc)]
      (if-not (lt/exist? doc-id)
        (try
          (resp/check (http/put url req) task state-key)
          (st/set-state! state-key :executed "add doc id endpoint and to lt-mem")
          (catch Exception e
            (st/set-state! state-key :error (.getMessage e))))
        (do
           (d/add mp-id doc-id)
           (st/set-state! state-key :executed "add doc id endpoint"))))))
