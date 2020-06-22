(ns cmp.worker.run-mp
  ^{:author "wactbprot"
    :doc "run-mp worker."}
  (:require [taoensso.timbre :as timbre]
            [cmp.st-mem :as st]
            [cmp.config :as cfg]))

(def mtp (cfg/min-task-period (cfg/config)))
(comment
  (defn exec-index
    [{mp :Mp mp-id :MpName cont :Container state-key :StateKey}]
    )
  
  (defn run-mp!
    [task]
    (let [{cont-title :ContainerTitle
           cont-index :Container
         state-key  :StateKey} task]
      (when state-key
        (st/set-val! state-key "working")
        (timbre/debug "start with wait, already set " state-key  " working"))
      (cond
        (not (nil? cont-title)) (exec-title task)
        (not (nil? cont-index)) (exec-index task)
        :not-found (when state-key
                   (timbre/error (str "no container title or index at: " state-key))
                   (st/set-val! state-key "error"))))))