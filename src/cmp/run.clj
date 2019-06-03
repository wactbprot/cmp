(ns cmp.run
  ^{:author "wactbprot"
    :doc "Runs the upcomming tasks of a certain container."}
  (:require [taoensso.timbre :as log]
            [cmp.st :as st]
            [cmp.utils :as u])
  (:gen-class))

(defn executed?
  [k]
  (= (st/get-val k)
     "executed"))

(defn ready?
  [k]
  (= (st/get-val k)
     "ready"))

(defn same-idx-fn
  [idx]
  (fn [k]
    (= (u/key->seq-idx k)
       idx)))

(defn successor-idx-fn
  [idx]
  (fn [k]
    (= (u/key->seq-idx k)
       idx + 1)))

(defn extr-next
  "Extracts the next tasks to run.
  Todo: needs to check if all tasks before next ready are executed"
  [p i]
  (let [ks  (sort (st/get-keys (u/get-state-path p i)))
        exec-ks (filter executed? ks)
        ready-ks (filter ready? ks)
        last-exec-idx (u/key->seq-idx (last exec-ks))
        next-ready-idx (u/key->seq-idx (first ready-ks))
        same-idx? (same-idx-fn next-ready-idx)
        next-ks (filter same-idx? ready-ks)]
    next-ks
    ))
