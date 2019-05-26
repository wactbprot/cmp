(ns cmp.run
  ^{:author "wactbprot"
    :doc "Runs the tasks of a certain container."}
  (:require [taoensso.timbre :as log]
            [cmp.st :as st]
            [cmp.utils :as u])
  (:gen-class))

(defn run
  [p i]
  (let [path [p "container" i "state"]
        ks  (st/get-keys (u/gen-key path))]
    (println ks)))