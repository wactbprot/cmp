(ns cmp.poll
  ^{:author "wactbprot"
    :doc "Polls short term memory endpoints 
          and reacts on result (:load, :run, :stop etc)."}
  (:require [taoensso.timbre :as log]
            [cmp.st :as st]
            [cmp.check :as chk]
            [cmp.run :as r]
            [cmp.utils :as u])
  (:gen-class))

(log/set-level! :debug)

(def heartbeat 500)
(def future-calls
  (atom {}))

;;------------------------------
;; register
;;------------------------------
(defn register
  [p f]
  (swap! future-calls assoc p f))

(defn registered?
  [p]
  (contains? @future-calls p))

;;------------------------------
;; dispatch
;;------------------------------
(defmulti dispatch
  "The load cmd now leads to a check since 
   the recipe concept is droped"
  (fn [s p i]
    (keyword (u/get-next-ctrl s))))

(defmethod dispatch :run
  [s p i]
 (let [ctrl-path (u/get-ctrl-path p i)
        ctrl-str-before (u/set-next-ctrl s "runing")]
    (dosync
     (st/set-val! ctrl-path ctrl-str-before)
     (r/trigger-next p i))))

(defmethod dispatch :runing
  [s p i]
  (r/trigger-next p i))

(defmethod dispatch :load
  [s p i]
  (let [ctrl-path (u/get-ctrl-path p i)
        ctrl-str-before (u/set-next-ctrl s "checking")
        ctrl-str-after (u/rm-next-ctrl s)]
    (dosync
     (st/set-val! ctrl-path ctrl-str-before)
     (chk/container p i)
     (st/set-val! ctrl-path ctrl-str-after))))

(defmethod dispatch :default
  [s p i])

;;------------------------------
;; monitor
;;------------------------------
(defn monitor
  [p i]
  (future
      (while true
        (do
          (Thread/sleep heartbeat)
          (let [ctrl-str (st/get-val (u/get-ctrl-path p i))]
            (dispatch ctrl-str p i))))))

;;------------------------------
;; start
;;------------------------------
(defmulti start
  (fn [p i] (registered? (u/get-ctrl-path p i))))

(defmethod start true
  [p i])

(defmethod start false
  [p i]
  (dosync 
   (let [ctrl-path (u/get-ctrl-path p i)
         f (monitor p i)]
     (register ctrl-path f))))
;;------------------------------
;; stop
;;------------------------------
(defmulti stop 
  (fn [p i] (registered? (u/get-ctrl-path p i))))

(defmethod stop false
  [p i])

(defmethod stop true
  [p i]
  (dosync
   (let [ctrl-path (u/get-ctrl-path p i)]     
     (future-cancel (@future-calls ctrl-path))
     (swap! future-calls dissoc ctrl-path))))

;;------------------------------
;; status
;;------------------------------
(defn status
  []
  (println @future-calls))