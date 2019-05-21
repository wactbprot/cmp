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
          (extract-cmds (st/get-val ctrl-path) ctrl-path))))))

(defn register
  [p f]
  (swap!
   future-calls
   assoc p f))

(defn registered?
  [p]
  (contains? @future-calls p))


(defn stop-cont-mon
  [p]
  (future-cancel (@future-calls p)))

(defn extract-cmds
  "Extracts commands.
  Enables kind of programming like provided in ssmp:
  load;run;stop --> [load, run, stop]
  load;2:run,stop -->  [load, run, stop, run, stop]"
  [cmds]
  (println cmds) 
