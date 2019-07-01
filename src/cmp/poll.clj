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

(def heartbeat 500)
(def future-calls
  (atom {}))

;;------------------------------
;; register
;;------------------------------
(defn register
  [ctrl-path f]
  (swap! future-calls assoc ctrl-path f))

(defn registered?
  [ctrl-path]
  (contains? @future-calls ctrl-path))

;;------------------------------
;; dispatch
;;------------------------------
(defmulti dispatch
  "The load cmd now leads to a check since 
   the recipe concept is droped"
  (fn [ctrl-str ctrl-path]
    (keyword (u/get-next-ctrl ctrl-str))))

(defmethod dispatch :run
  [ctrl-str ctrl-path]
 (let [ctrl-str-before (u/set-next-ctrl ctrl-str "runing")]
    (dosync
     (st/set-val! ctrl-path ctrl-str-before)
     (r/trigger-next ctrl-path))))

(defmethod dispatch :runing
  [ctrl-str ctrl-path]
  (r/trigger-next ctrl-path))

(defmethod dispatch :load
  [ctrl-str ctrl-path]
  (let [ctrl-str-before (u/set-next-ctrl ctrl-str "checking")
        ctrl-str-after (u/rm-next-ctrl ctrl-str)]
    (dosync
     (st/set-val! ctrl-path ctrl-str-before)
     (chk/container (u/key->mp-name ctrl-path) (u/key->no-idx ctrl-path))
     (st/set-val! ctrl-path ctrl-str-after))))

(defmethod dispatch :default
  [ctrl-str ctrl-path])

;;------------------------------
;; monitor
;;------------------------------
(defn monitor
  [ctrl-path]
  (future
      (while true
        (do
          (Thread/sleep heartbeat)
          (let [ctrl-str (st/get-val ctrl-path)]
            (dispatch ctrl-str ctrl-path))))))

;;------------------------------
;; start
;;------------------------------
(defmulti start
  (fn [ctrl-path] (registered? ctrl-path)))

(defmethod start true
  [ctrl-path])

(defmethod start false
  [ctrl-path]
  (register ctrl-path (monitor ctrl-path)))

;;------------------------------
;; stop
;;------------------------------
(defmulti stop 
  (fn [ctrl-path](registered? ctrl-path)))

(defmethod stop false
  [ctrl-path])

(defmethod stop true
  [ctrl-path]
  (dosync
   (future-cancel (@future-calls ctrl-path))
     (swap! future-calls dissoc ctrl-path)))

;;------------------------------
;; status
;;------------------------------
(defn f-calls
  []
  @future-calls)
