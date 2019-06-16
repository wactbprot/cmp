(ns cmp.check
  (:require [cmp.utils :as u]
            [cmp.st :as st]
            [cmp.task :as tsk]
            [taoensso.timbre :as log])
  (:use [clojure.repl])
  (:gen-class))

(defn container
  "checks the tasks of the i-th container which means:
  1) the proto task is taken from the definition section of the (build mp)
  2) the meta task is gathered from different sources
  3) the state at the path is set to 'checking'
  4) the meta-task is checked against the spec
  5) the state is set to 'ready'"   
  [p i]
  (let [ks (st/get-keys (u/get-defin-path p i))]
    (mapv
     (fn [k]
       (let [state-key (u/replace-key-at-level 3 k "state")
             proto-task (u/gen-map (st/get-val k))
             meta-task (tsk/gen-meta-task proto-task)]
         (st/set-val! state-key "checking")
         (assert (tsk/meta-task? meta-task))
         (st/set-val! state-key "ready")))
     ks)))
