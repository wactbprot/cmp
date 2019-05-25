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

(defn register
  [p f]
  (swap! future-calls assoc p f))

(defn registered?
  [p]
  (contains? @future-calls p))

(defmulti disp
  (fn [s p i]
    s))

(defmethod disp "run"
  [s p i]
  (println "run"))

(defmethod disp :default
  [s p i]
  (println "default"))

(defn mon
  [p i]
  (future
      (while true
        (do
          (Thread/sleep heartbeat)
          (let [ctrl-path (get-ctrl-path p i)
                ctrl-str (st/get-val ctrl-path)
                ctrl-val (u/next-ctrl ctrl-str)]
                                        ;(disp "run" p i)
            )
          ))))

(defmulti start
  (fn [p i] (registered? (get-ctrl-path p i))))

(defmethod start true
  [p i]
  nil)

(defmethod start false
  [p i]
  (dosync 
   (let [ctrl-path (get-ctrl-path p i)
         f (mon p i)]
     (register ctrl-path f))))

(defmulti stop 
  (fn [p i] (registered? (get-ctrl-path p i))))

(defmethod stop true
  [p i]
  (dosync
   (let [ctrl-path (get-ctrl-path p i)]     
     (future-cancel (@future-calls ctrl-path))
     (swap! future-calls dissoc ctrl-path))))

(defmethod stop false
  [p i]
  nil)
