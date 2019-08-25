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
;; ctrl channel invoked by poll 
;;------------------------------
(def ctrl-chan (a/chan))

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

(defn ks->state-map
  [ks]
  (mapv
   (fn [k]
     {:seq-idx (u/key->seq-idx k)
      :par-idx (u/key->par-idx k)
      :state (st/get-val k)})
   ks))

(defn filter-state
  [v s]
  (filter (fn [m] (= s (m :state))) v))
  
(defn all-error
  [v]
  (filter-state v "error"))

(defn all-ready
  [v]
  (filter-state v "ready"))

(defn all-working
  [v]
  (filter-state v "working"))

(defn all-executed
  [v]
  (filter-state v "executed"))

(defn no-error?
  [v]
  (empty? (all-error v)))

(defn all-ready?
  [v]
  (=
   (count v)
   (count (all-ready v))))

(defn next-ready
  [v]
  (first all-ready))

(defn trigger-next
  [p]
  (let [v (st/get-val p)]
    (st/set-val! p "checking")
    (let [state-path (u/replace-key-at-level 3 p "state")
          ks (sort (st/get-keys state-path))
          state-map (ks->state-map ks)]
      (println state-map))
    (st/set-val! p v)))

;;------------------------------
;; worker 
;;------------------------------
(defmulti worker
  (fn [task] (task :Action)))

(defmethod worker :Wait
  [task]
  (println "wait"))

(defmethod worker :default
  [task]
  (println (task :Action)))


;;------------------------------
;; ctrl go block 
;;------------------------------
(a/go
  (while true  
    (let [p (a/<! trigger-chan)] 
      (log/info "got trigger for " p)
      (trigger-next p))))
