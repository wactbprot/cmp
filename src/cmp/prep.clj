(ns cmp.prep
  (:require [cmp.utils :as u]
            [cmp.st :as st]
            [cmp.task :as t]
            [cmp.lt :as lt]
            [taoensso.timbre :as log])
  (:use [clojure.repl])
  (:gen-class))

(log/set-level! :debug)

(defn container [path i]
  (let [definition-keys (st/get-keys
                         (u/gen-key [path "container" i "definition"]))]
    (doall 
     (map
      (fn [k]
        (let [state-key (u/replace-key-at-level 3 k "state")
              proto-task (u/gen-map (st/get-val k))
              {id :id key :key db-task :value} (lt/get-task-view proto-task)
              globals (t/global-defaults path)]
          (log/info "try to prepair task for key: " k)
          (log/debug "task is:" db-task)
          (assert (t/task? db-task))
          (st/set-val! state-key "prepairing")
          (t/assemble db-task proto-task globals)))
      definition-keys))))

