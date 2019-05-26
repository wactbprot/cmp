(ns cmp.prep
  (:require [cmp.utils :as u]
            [cmp.st :as st]
            [cmp.task :as tsk]
            [cmp.lt :as lt]
            [taoensso.timbre :as log])
  (:use [clojure.repl])
  (:gen-class))

(defn container
  "Prepairs the i-th container which means:
  1) the proto task is taken from the definition section of the (build mp)
  2) the meta task is gathered from different sources
  3) the task is checked against the spec
  4) the state at the path is set to 'preparing'
  5) the meta task is assembled to a runnable task
  6) the runnable task is stored in the short term memory
  7) the state is set to 'ready'"   
  [p i]
  (let [path [p "container" i "definition"]
        ks (st/get-keys (u/gen-key path))]
    (mapv
     (fn [k]
       (let [state-key (u/replace-key-at-level 3 k "state")
             recipe-key (u/replace-key-at-level 3 k "recipe")
             proto-task (u/gen-map (st/get-val k))
             temps (tsk/get-temps p)
             meta-task (assoc (tsk/gen-meta-task proto-task) :Temps temps)]
         (assert (tsk/task? (:Task meta-task)))
         (st/set-val! state-key "prepairing")
         (st/set-val! recipe-key (u/gen-value (tsk/assemble meta-task)))
         (st/set-val! state-key "ready")
         ))
     ks)))
