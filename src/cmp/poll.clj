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
            [cmp.run :as run]
            [cmp.utils :as u])
  (:gen-class))

(def heartbeat 1000)
(def mon (atom {}))
(def excep-chan (a/chan))

;;------------------------------
;; register
;;------------------------------
(defn register
  [p]
  (log/info "register channel for path: " p)
  (swap! mon assoc p true))

(defn registered?
  [p]
  (contains? @mon p))

;;------------------------------
;; exception channel 
;;------------------------------
(a/go
  (while true
    (let [e (a/<! excep-chan)] 
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
  (st/set-val! ctrl-path "running")
  (a/>!! run/trigger-chan ctrl-path))

(defmethod dispatch :running
  [ctrl-str ctrl-path]
  (log/info ".")
  (a/>!! run/trigger-chan ctrl-path))

(defmethod dispatch :default
  [ctrl-str ctrl-path])

;;------------------------------
;; monitor
;;------------------------------
(defn monitor
  [p]
  (log/info "start go block for observing ctrl path: " p)
  (a/go
    (while ((deref mon) p)
      (a/<! (a/timeout heartbeat))
      (try
        (println ".")
        (dispatch (st/get-val p) p)
        (catch Exception e
          (log/error "catch error at channel " p)
          (a/>! excep-chan e))))))

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
  (register p)
  (monitor p)
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
   (swap! mon assoc p false)
   (log/info "close monitor channel registered for path: " p))
