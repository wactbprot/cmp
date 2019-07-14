(ns cmp.poll
  ^{:author "wactbprot"
    :doc "Polls the short term memory endpoint `ctrl` 
          and dispatchs depending on the result 
          (:load, :run, :stop etc)."}
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
(def mon-chans (atom {}))

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
;; register
;;------------------------------
(defn register
  [p c]
  (swap! mon-chans assoc p c))

(defn registered?
  [p]
  (contains? @mon-chans p))

;;------------------------------
;; exception channel 
;;------------------------------
(a/go
  (while (evaluate-condition)  
    (let [e (a/<! exception-chan)] 
      (log/error (.getMessage e)))))

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
;; monitor
;;------------------------------
(defn monitor
  [p]
  (log/info "start go block for observing ctrl path: " p)
  (a/go
    (while (evaluate-condition)
      (a/<! (a/timeout heartbeat))
      (try 
        (dispatch (st/get-val p) p)
        (catch Exception e
          (log/error "catch error at channel " p)
          (a/>! exception-chan e))))))

;;------------------------------
;; start
;;------------------------------
(defmulti start
  (fn [p] (registered? p)))

(defmethod start true
  [p]
  (log/info "monitor channel for path: " p " already registered"))

(defmethod start false
  [p]
  (register p (monitor p))
  (log/info "start and register monitor channel for path: " p))

;;------------------------------
;; stop
;;------------------------------
(defmulti stop 
  (fn [p](registered? p)))

(defmethod stop false
  [p]
  (log/info "no monitor channel registered for path: " p))

(defmethod stop true
  [p]
  (dosync
   (a/close! (@mon-chans p))
   (swap! mon-chans dissoc p)
   (log/info "close monitor channel registered for path: " p)))
