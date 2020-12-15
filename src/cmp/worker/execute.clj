(ns cmp.worker.execute
  ^{:author "wactbprot"
    :doc "The worker for the execute action."}
  (:require [cmp.config :as cfg]
            [cmp.resp :as resp]
            [cmp.st-mem :as st]
            [cmp.utils :as u]
            [cmp.key-utils :as ku]
            [taoensso.timbre :as log]
            [clojure.java.shell :refer [sh]]))

(defn execute!
  [task]
    (let [state-key   (:StateKey task)]
    (st/set-state! state-key :working)))
