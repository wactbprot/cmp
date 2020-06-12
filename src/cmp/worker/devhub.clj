(ns cmp.worker.devhub
  ^{:author "wactbprot"
    :doc "devhub worker."}
  (:require [clj-http.client :as http]
            [clojure.core.async :as a]
            [cmp.config :as cfg]
            [cmp.resp :as resp]
            [cmp.st-mem :as st]
            [cmp.utils :as u]
            [cmp.utils :as u]
            [cmp.worker.pre-script :as ps]
            [taoensso.timbre :as timbre]))

(def mtp (cfg/min-task-period (cfg/config)))
(def post-header (cfg/post-header (cfg/config)))
(def dev-hub-url (cfg/dev-hub-url (cfg/config)))


(defn resolve-pre-script
  "Checks if the task has a `:PreScript` (name of the script to run)
  and an `:Input` key. If not `task` is returned."
  [task state-key]
  (if-let [script-name (:PreScript task)]
    (if-let [input (:PreInput task)]
      (condp = script-name
        "set_valve_pos" (ps/set-valve-pos task)
        "get_valve_pos" (ps/get-valve-pos task)
        (do
          (timbre/error "script with name: " script " not implemented")
          (st/set-val! state-key "error")
          (timbre/error "set state: " state-key " to error")))
      task)
    task))

(defn devhub!
  "Param is called `pre-task` because some tasks come with a
  `:PreScript` which has to be executed in order to complete
  the task (sometimes the `:Value` is computed be the
  `:PreScript`).
  
  ```clojure
  
   (devhub! ((meta (var devhub!)) :example-task)
            ((meta (var devhub!)) :example-state-key))
  ```"
  {:example-state-key "example"
   :example-task 
   {
    :TaskName "VS_NEW_SE3-set_valve_pos"
    :Comment "Setzt die Ventilposition."
    :Action "MODBUS"
    :StateKey "example@container@0@state@0@1"
    :MpName "devhub"
    :Host "172.30.56.46"
    :FunctionCode "writeSingleRegister"
    :PreInput
    {
     :should "open"
     :valve "V1"
     :stateblock1 [1 0 1 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 1 0]
     :stateblock2 [0 0 1 0 0 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0]
     :stateblock3 [0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 1 1 1 0 0 0 0 0]
     :stateblock4 [0 0 0 0 0 0 0 0 1 1 1 0 0 0 0 0 0 0 0 0 0 0 0 0]
     }
    :PreScript "set_valve_pos"
    }
   }
  [pre-task state-key]
  (st/set-val! state-key "working")
  (Thread/sleep mtp)
  (if-let [task (resolve-pre-script pre-task state-key)]
    (let [req (assoc post-header :body (u/map->json task))
          url dev-hub-url]
      (timbre/debug "send req to: " url)
      (a/go
        (resp/check (http/post url req) task state-key)))
    (do 
      (timbre/error (str "failed to build task for: " state-key))
      (st/set-val! state-key "error"))))
