(ns cmp.run
  ^{:author "wactbprot"
    :doc "Runs the upcomming tasks of a certain container."}
  (:require [taoensso.timbre :as log]
            [cmp.st :as st]
            [cmp.utils :as u])
  (:gen-class))

(defn find-next
  "Finds the next tasks to run."
  [p i]
  (let [ks  (sort (st/get-keys (u/get-state-path p i)))
        ready-ks (filter
                  (fn [k]
                    (= (st/get-val k)
                       "ready"))
                  ks)
        next-idx (u/key->seq-idx (first ready-ks))
        next-ks (filter
                 (fn [k]
                   (= (u/key->seq-idx k)
                      next-idx))
                 ready-ks)]
    next-ks
    ))
