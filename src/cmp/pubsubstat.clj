(ns cmp.pubsubstat
  ^{:author "wactbprot"
    :doc "Prints a table wthi the n latest pub sub events."}
  (:require [cmp.st-mem :as st]
            [clojure.core.async :as a]
            [cmp.utils :as u]
            [clojure.pprint :as pp]))

(def table (atom []))

(defn cb!
  [msg]
  (let [d   (u/get-date-object)
        k   (st/msg->key msg)
        val (st/key->val k)]
    (swap! table conj {:h (u/get-hour d)
                       :m (u/get-min d)
                       :s (u/get-sec d)
                       :meth (nth msg 0)
                       :k k
                       :val val })
    (pp/print-table (deref table))))

(defn start
  "Registers a listener. Pretty prints a table
  on events."
  ([]
   (start "*" "*" "*" "*"))
  ([mp-id struct]
   (start mp-id struct "*" "*"))
  ([mp-id struct i]
   (start mp-id struct i "*"))
  ([mp-id struct i func]
  (st/register! mp-id struct i func cb!)))

(defn stop
  "De-registers the listener. Resets the table `atom`."
  ([]
   (stop "*" "*" "*" "*"))
  ([mp-id struct]
   (stop mp-id struct "*" "*"))
  ([mp-id struct i]
   (stop mp-id struct i "*"))
  ([mp-id struct i func]
   (reset! table [])
   (st/de-register! mp-id struct i func)))