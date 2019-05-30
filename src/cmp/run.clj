(ns cmp.run
  ^{:author "wactbprot"
    :doc "Runs the tasks of a certain container."}
  (:require [taoensso.timbre :as log]
            [cmp.st :as st]
            [cmp.utils :as u])
  (:gen-class))

(defn run
  [p i]
  (let [ks  (sort (st/get-keys (u/get-state-path p i)))
        state-ready (filter
                     (fn [k]
                       (= (st/get-val k) "ready"))
                     ks)
        next-idx (u/extr-seq-idx (first state-ready))
        next-ks (filter
                 (fn [k]
                   (= (u/extr-seq-idx k) next-idx))
                 ks)]
    next-ks
    ))