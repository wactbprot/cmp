(ns cmp.worker.devhub
  ^{:author "wactbprot"
    :doc "devhub worker."}
  (:require [clj-http.client :as http]
            [clojure.core.async :as a]
            [cmp.config :as cfg]
            [cmp.resp :as resp]
            [cmp.st-mem :as st]
            [cmp.utils :as u]
            [taoensso.timbre :as log]
            [cmp.worker.pre-script :as ps]
            [taoensso.timbre :as timbre]))

(def post-header (cfg/post-header (cfg/config)))
(def dev-hub-url (cfg/dev-hub-url (cfg/config)))

(defn resolve-pre-script
  "Checks if the task has a `:PreScript` (name of the script to run)
  and an `:Input` key. If not, `task` is returned."
  [task]
  (let [{script-name :PreScript
         input       :PreInput
         state-key   :StateKey} task]
    (if (string? script-name)
      (if (string? input)
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
  (let [state-key (:StateKey pre-task)]
    (st/set-state! state-key :working)
    (if-let [task (resolve-pre-script pre-task)]
      (let [req  (assoc post-header :body (u/map->json task))
            url  dev-hub-url]
        (log/debug "try to send req to: " url)
        (a/go
          (try
            (let [res (http/post url req)]
              (resp/check res task state-key))
            (catch Exception e
              (st/set-state! state-key :error)
              (log/error "request failed")))))
      (do 
        (log/error (str "failed to exec task at: " state-key))
        (st/set-state! state-key :error)))))
