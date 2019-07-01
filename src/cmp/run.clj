(ns cmp.run
  ^{:author "wactbprot"
    :doc "Runs the upcomming tasks of a certain container."}
  (:require [taoensso.timbre :as log]
            [cmp.st :as st]
            [cmp.task :as tsk]
            [cmp.utils :as u])
  (:gen-class))


(defn par-start
  [ks]
  (run!
   (fn [k]
     (let [recipe-key (u/replace-key-at-level 3 k "recipe")
           task (u/gen-map (st/get-val recipe-key))]
       (println task)
       ;; (tsk/dyn-assemble
      ;;  (assoc task
      ;;         :Mp (u/key->mp-name k)
      ;;         :No (u/key->no-idx k)
      ;;         :Seq (u/key->seq-idx k)
      ;;         :Par (u/key->par-idx k)))))
       ks))))

(defn executed?
  [k]
  (= (st/get-val k)
     "executed"))

(defn ready?
  [k]
  (= (st/get-val k)
     "ready"))

(defn par-idx-fn
  [idx]
  (fn [k]
    (= (u/key->seq-idx k)
       idx)))

(defn first-or-successor-idx-fn
  [idx]
  (fn [k]
    (or
     (= (u/key->seq-idx k)
        0)
     (= (u/key->seq-idx k)
        (+ idx 1)))))

(defn trigger-next
  "Extracts the next tasks to run.
  1) get all state keys of the container
  2) sort
  3) filter out all executed ones
  4) filter out all ready ones
  5) get the last executed idx
  6) get the next ready idx
  7) generate filter fns:
  7a) par-idx? with the idx of the next-ready-idx and
  7b) first-or-successor-idx? with the idx of the last-exec-idx
  8) filter on 7a&b fns"
  [p i]
  (let [ks  (sort (st/get-keys (u/get-cont-state-path p i)))
        exec-ks (filter executed? ks)
        ready-ks (filter ready? ks)
        last-exec-idx (u/key->seq-idx (last exec-ks))
        next-ready-idx (u/key->seq-idx (first ready-ks))
        par-idx? (par-idx-fn next-ready-idx)
        first-or-successor-idx? (first-or-successor-idx-fn last-exec-idx)
        next-par-ks (filter first-or-successor-idx?
                            (filter par-idx? ready-ks))]
    (par-start next-par-ks)))
