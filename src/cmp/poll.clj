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

(defn start-cont-mon
  [p i]
  (let [ctrl-path (u/gen-key [p "container" i "ctrl"])]
    (future
      (while true
        (do
          (Thread/sleep heartbeat)
          (println (st/get-val ctrl-path)))))))

(defn register [p f]
  (swap!
   future-calls
   assoc p f))


(defn stop-cont-mon
  [p]
  (future-cancel (@future-calls p)))