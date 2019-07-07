(ns cmp.poll
  ^{:author "wactbprot"
    :doc "Polls short term memory endpoints 
          and reacts on result (:load, :run, :stop etc)."}
  (:require [clojure.string :as string]
            [taoensso.timbre :as log]
            [clojure.core.async :as a]
            [cmp.st :as st]
            [cmp.check :as chk]
            [cmp.run :as r]
            [cmp.utils :as u])
  (:gen-class))

(def heartbeat 1000)
(def poll-condition (atom true))
(def exception-chan (a/chan))
(defn disable-monitor
  []
  (reset! poll-condition false ))

(defn enable-monitor
  []
  (reset! poll-condition true ))

(defn evaluate-condition
  []
  @poll-condition)

;;------------------------------
;; dispatch
;;------------------------------
(defmulti dispatch
  (fn [ctrl-str ctrl-path]
    (keyword (u/get-next-ctrl ctrl-str))))

(defmethod dispatch :run
  [ctrl-str ctrl-path]
  (log/info "start running: " ctrl-path)
  (let [ctrl-str-before (u/set-next-ctrl ctrl-str "running")]
    (dosync
     (st/set-val! ctrl-path ctrl-str-before)
     (r/trigger-next ctrl-path))))

(defmethod dispatch :running
  [ctrl-str ctrl-path]
  (r/trigger-next ctrl-path))

(defmethod dispatch :default
  [ctrl-str ctrl-path])


;;------------------------------
;; exception 
;;------------------------------
(a/go
  (let [e (a/<! exception-chan)] 
    (log/error (.getMessage e)))

;;------------------------------
;; monitor
;;------------------------------
(defn monitor
  [ctrl-path]
  (a/go
    (while (evaluate-condition)
      (a/<! (a/timeout heartbeat))
      (try 
        (log/info ctrl-path)
        (dispatch (st/get-val ctrl-path) ctrl-path)
        (catch Exception e
          (a/>! exception-chan e))))))


