(ns cmp.prep
  (:require [cmp.utils :as u]
            [cmp.st :as st]
            [cmp.task :as t]
            [cmp.lt :as lt]
            [taoensso.timbre :as log])
  (:use [clojure.repl]);; enables e.g. (doc .)
  (:gen-class))


(log/set-level! :error)

(defn container [path i]
  (let [definition-keys (st/get-keys
                         (u/gen-key [path "container" i "definition"]))]
    (map
     (fn [k]
       (let [state-key (u/replace-key-level 3 k "state")
             proto-task (u/gen-map (st/get-val k))
             {id :id key :key db-task :value} (lt/get-task-view proto-task)]
         (log/info "try to prepair task for key: " k)
         (log/debug "task is:" db-task)
         (t/task? db-task) 
         (st/set-val state-key "start-prep")
         (t/assemble proto-task db-task)
         ))
     definition-keys))) 

