(ns cmp.poll
  ^{:author "wactbprot"
    :doc "Polls short term memory endponts and reacts on result."}
  (:require [taoensso.timbre :as log]
            [cmp.st :as st]
            [cmp.prep :as p]
            [cmp.utils :as u])
  (:gen-class))

(log/set-level! :debug)

(def heartbeat 500)
(def future-calls
  (atom {}))

(defn register
  [p f]
  (swap! future-calls assoc p f))

(defn registered?
  [p]
  (contains? @future-calls p))

(defmulti dispatch
  (fn [s p i]
    (keyword (u/get-next-ctrl s))))

(defmethod dispatch :run
  [s p i]
  (println "run"))

(defmethod dispatch :load
  [s p i]
  (let [ctrl-path (u/get-ctrl-path p i)
        ctrl-str-before (u/set-next-ctrl s "loading")
        ctrl-str-after (u/rm-next-ctrl s)]
    (println ctrl-str-before)
    (dosync

     (st/set-val! ctrl-path ctrl-str-before)
     (println (p/container p i))
     (println ctrl-str-after)
     (st/set-val! ctrl-path ctrl-str-after))))

(defmethod dispatch :default
  [s p i]
  (println s))

(defn monitor
  [p i]
  (future
      (while true
        (do
          (Thread/sleep heartbeat)
          (let [ctrl-str (st/get-val (u/get-ctrl-path p i))]
            (dispatch ctrl-str p i))))))

(defmulti start
  (fn [p i] (registered? (u/get-ctrl-path p i))))

(defmethod start true
  [p i]
  nil)

(defmethod start false
  [p i]
  (dosync 
   (let [ctrl-path (u/get-ctrl-path p i)
         f (monitor p i)]
     (register ctrl-path f))))

(defmulti stop 
  (fn [p i] (registered? (u/get-ctrl-path p i))))

(defmethod stop true
  [p i]
  (dosync
   (let [ctrl-path (u/get-ctrl-path p i)]     
     (future-cancel (@future-calls ctrl-path))
     (swap! future-calls dissoc ctrl-path))))

(defmethod stop false
  [p i]
  nil)
