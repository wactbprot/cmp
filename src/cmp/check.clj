(ns cmp.check
  (:require [cmp.utils :as u]
            [cmp.st :as st]
            [cmp.task :as tsk]
            [taoensso.timbre :as log])
  (:use [clojure.repl])
  (:gen-class))

(defn struct
  "Checks the tasks of the structure (container or definitions):
  1) the proto task is taken from the definition section of the (build mp)
  2) the meta task is gathered from different sources
  3) the state at the path is set to 'checking'
  4) the meta-task is checked against the spec
  5) the state is set to 'ready'"   
  [path]
  (let [ks (st/get-keys path)]
    (run!
     (fn [k]
       (let [state-key (u/replace-key-at-level 3 k "state")
             proto-task (u/gen-map (st/get-val k))
             meta-task (tsk/gen-meta-task proto-task)]
         (st/set-val! state-key "checking")
         (assert (tsk/meta-task? meta-task))
         (st/set-val! state-key "ready")))
     ks)))
