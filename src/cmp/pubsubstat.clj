(ns cmp.pubsubstat
  ^{:author "wactbprot"
    :doc "Prints a table wthi the n latest pub sub events."}
  (:require [cmp.st-mem :as st]
            [clojure.core.async :as a]
            [cmp.utils :as u]
            [clojure.pprint :as pp]))

(def table (atom []))

(defn start
  [mp-id]
  (st/register! mp-id "*" "*" "*" (fn [msg]
                                    (prn msg)
                                    (let [d   (u/get-date-object)
                                          k   (st/msg->key msg)
                                          val (st/key->val k)]
                                      (swap! table conj {:h (u/get-hour d)
                                                         :m (u/get-min d)
                                                         :s (u/get-sec d)
                                                         :meth (nth msg 0)
                                                         :k k
                                                         :val val })
                                      (pp/print-table (deref table))))))
(defn stop
  [mp-id]
  (reset! table [])
  (st/de-register! mp-id "*" "*" "*"))