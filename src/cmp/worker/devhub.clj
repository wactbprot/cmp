(ns cmp.worker.devhub
  ^{:author "wactbprot"
    :doc "The devhub worker."}
  (:require [clj-http.client :as http]
            [cmp.config :as cfg]
            [cmp.resp :as resp]
            [cmp.st-mem :as st]
            [cmp.utils :as u]
            [taoensso.timbre :as log]
            [cmp.worker.pre-script :as ps]))

(defn resolve-pre-script
  "Checks if the task has a `:PreScript` (name of the script to run)
  and an `:Input` key. If not, `task` is returned."
  [task]
  (let [{script-name :PreScript
         input       :PreInput
         state-key   :StateKey} task]
    (if (string? script-name)
      (if (map? input)
        (condp = (keyword script-name)
          :set_valve_pos (ps/set-valve-pos task)
          :get_valve_pos (ps/get-valve-pos task)
          (do
            (log/error "script: " script-name " not implemented")
            (st/set-state! state-key :error)))
        task)
      task)))
  
(defn devhub!
  "Param is called `pre-task` because some tasks come with a
  `:PreScript` which has to be executed in order to complete
  the task (sometimes the `:Value` is computed by the
  `:PreScript`).
  
  ```clojure
   (devhub! {:Action \"TCP\" :Port 23 :Host \"localhost\" :Value \"Hi!\"})
  ```"
  [pre-task]
  (let [state-key   (:StateKey pre-task)]
    (st/set-state! state-key :working)
    (let [request-key (st/state-key->request-key state-key)
          task        (resolve-pre-script pre-task)
          json-task   (u/map->json task)
          req         (assoc (cfg/json-post-header (cfg/config)) :body json-task)
          url         (cfg/dev-hub-url (cfg/config))]
      (log/debug "try to send req to: " url)
      (st/set-val! request-key json-task)
      (log/debug "stored json task to: " request-key)
      (try
        (resp/check (http/post url req) task state-key)
        (catch Exception e
          (st/set-state! state-key :error)
          (log/error "request failed"))))))
  
