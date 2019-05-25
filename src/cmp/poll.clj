(ns cmp.poll
  ^{:author "wactbprot"
    :doc "Polls short term memory endponts and reacts on result."}
  (:require [taoensso.timbre :as log]
            [cmp.st :as st]
            [cmp.utils :as u])
  (:gen-class))

(log/set-level! :debug)

(def heartbeat 500)
(def future-calls
  (atom {}))

(defn get-ctrl-path
  [p i]
  (u/gen-key [p "container" i "ctrl"]))

(defn mon
  [p]
  (future
      (while true
        (do
          (Thread/sleep heartbeat)
          (println (st/get-val p))))))

(defn register
  [p f]
  (swap!
   future-calls
   assoc p f))

(defn registered?
  [p]
  (contains? @future-calls p))

(defmulti start
  (fn [p i] (registered? (get-ctrl-path p i))))

(defmethod start true
  [p i]
  nil)

(defmethod start false
  [p i]
  (let [ctrl-path (get-ctrl-path p i)]
    (register ctrl-path (mon ctrl-path))))

(defmulti stop 
  (fn [p i] (registered? (get-ctrl-path p i))))

(defmethod stop true
  [p i]
  (future-cancel (@future-calls (get-ctrl-path p i))))

(defmethod stop false
  [p i]
  nil)

