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

(defn trigger-next
  [ctrl-path]
  (println ctrl-path)
  (let [ctrl-val (st/get-val ctrl-path)]
    (st/set-val! ctrl-path "checking")
    (println ctrl-path)    
    (let [state-path (u/replace-key-at-level 3 ctrl-path "state")
          ks (sort (st/get-keys state-path))
          state-map (ks->state-map ks)]
      (println state-map))
    (st/set-val! ctrl-path ctrl-val) 
    )
  )

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
;; trigger channel 
;;------------------------------
(def trigger-chan (a/chan))

(a/go
  (while (evaluate-condition)  
    (let [p (a/<! trigger-chan)] 
      (log/info "got trigger for " p)
      (trigger-next p))))
