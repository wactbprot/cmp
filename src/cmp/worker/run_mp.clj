(ns cmp.worker.run-mp
  ^{:author "wactbprot"
    :doc "run-mp worker."}
  (:require [taoensso.timbre :as log]
            [cmp.st-mem :as st]
            [cmp.utils :as u]
            [cmp.config :as cfg]))

(def mtp (cfg/min-task-period (cfg/config)))

(defn exec-index
  [{mp :Mp mp-id :MpName i :Container state-k :StateKey}]
  (let [ctrl-k (st/cont-ctrl-path mp i)]
    (st/register! mp-id "container" i "definition" (st/gen-listener-callback ctrl-k state-k))
    (st/set-val! ctrl-k "run")))


(defn exec-title
  [{mp :Mp mp-id :MpName cont :ContainerTitle state-key :StateKey}]
  )

(defn run-mp!
    [task]
    (let [{cont-title :ContainerTitle
           cont-index :Container
         state-key  :StateKey} task]
      (when state-key
        (st/set-val! state-key "working")
        (log/debug "start with wait, already set " state-key  " working"))
      (cond
        (not (nil? cont-title)) (exec-title task)
        (not (nil? cont-index)) (exec-index task)
        :not-found (when state-key
                     (log/error (str "no container title or index at: " state-key))
                     (st/set-val! state-key "error")))))