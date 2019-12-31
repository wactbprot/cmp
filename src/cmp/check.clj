(ns cmp.check
  (:require [cmp.utils :as u]
            [cmp.st-mem :as st]
            [cmp.task :as tsk]
            [taoensso.timbre :as log])
  (:use [clojure.repl]))

(defn struct-tasks
  "Checks the tasks of the structure (`container` or `definitions`):

  * the `proto-task` is taken from the definition section
  * the `meta-task` is gathered from different sources
  * the `state` at the path is set to `checking`
  * the `meta-task` is checked against the 
  * the `state` is set to `ready`"   
  [p]
  (run!
   (fn [k]
     (let [state-key (u/replace-key-at-level 3 k "state")
           proto-task (u/gen-map (st/key->val k))
           meta-task (tsk/gen-meta-task proto-task)]
       (st/set-val! state-key "checking")
       (assert (tsk/meta-task? meta-task))
       (st/set-val! state-key "ready")))
   (st/get-keys p)))
