(ns cmp.run
  ^{:author "wactbprot"
    :doc "Runs the upcomming tasks of a certain container."}
  (:require [taoensso.timbre :as log]
            [clojure.core.async :as a]
            [cmp.st :as st]
            [cmp.task :as tsk]
            [cmp.utils :as u])
  (:gen-class))


;;------------------------------
;; run condition
;;------------------------------
(def run-condition (atom true))
(defn disable-run
  []
  (reset! run-condition false ))

(defn enable-run
  []
  (reset! run-condition true ))

(defn evaluate-condition
  []
  @run-condition)

;;------------------------------
;; trigger channel 
;;------------------------------
(def trigger-chan (a/chan))

(a/go
  (while (evaluate-condition)  
    (let [p (a/<! trigger-chan)] 
        (trigger-next p))))


(defn par-run
  [ks]
  (run!
   (fn [k]
     (let [recipe-path (u/replace-key-at-level 3 k "definition")
           proto-task (u/gen-map (st/get-val recipe-path))
           meta-task (tsk/gen-meta-task proto-task)
           task (tsk/assemble meta-task)]
       (println (assoc task
                       :Mp (u/key->mp-name k)
                       :Struct (u/key->struct k)
                       :No (u/key->no-idx k)
                       :Seq (u/key->seq-idx k)
                       :Par (u/key->par-idx k)))))
   ks))

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
  [ctrl-path]
  (let [state-path (u/replace-key-at-level 3 ctrl-path "state")
        ks  (sort (st/get-keys state-path))
        exec-ks (filter executed? ks)
        ready-ks (filter ready? ks)
        last-exec-idx (u/key->seq-idx (last exec-ks))
        next-ready-idx (u/key->seq-idx (first ready-ks))
        par-idx? (par-idx-fn next-ready-idx)
        first-or-successor-idx? (first-or-successor-idx-fn last-exec-idx)
        next-par-ks (filter first-or-successor-idx?
                            (filter par-idx? ready-ks))]
    (par-run next-par-ks)))

(defmulti worker
  (fn [task] (task :Action)))

(defmethod worker :Wait
  [task]
  (println "wait"))

(defmethod worker :default
  [task]
  (println (task :Action)))