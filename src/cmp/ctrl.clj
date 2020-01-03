(ns cmp.ctrl
  ^{:author "wactbprot"
    :doc "Observes the ctrl interface."}
  (:require [taoensso.timbre :as timbre]
            [cmp.st-mem :as st]
            [clojure.core.async :as a]
            [cmp.reg :as reg]
            [cmp.state :as state]
            [cmp.utils :as u]))

;;------------------------------
;; ctrl-dispatch!, start, stop
;;------------------------------
(defn stop
  "De-registers a listener for the `ctrl`
  interface of the `mp-id`."
  [mp-id]
  (reg/de-register! mp-id "*" "*" "ctrl"))

(defn dispatch!
  "Dispatches on the ctrl path `p`."
  [p]
  (let [mp-id (u/key->mp-name p)
        cmd (->> p
                 (st/key->val)
                 (u/get-next-ctrl))]
    (cond
      (= cmd "run")     (a/>!! state/ctrl-chan [p "start"])
      (= cmd "mon")     (a/>!! state/ctrl-chan [p "start"])
      (= cmd "suspend") (a/>!! state/ctrl-chan [p "stop"])
      (= cmd "stop")    (do
                          (a/>!! state/ctrl-chan [p "stop"])
                          (stop mp-id))
      :default (timbre/debug "dispatch default branch for key: " p))))

(defn start
  "Registers a listener for the `ctrl` interface of the `mp-id`.
  The [[dispatch]] function becomes the `callback`." 
  [mp-id]
  (reg/register! mp-id "*" "*" "ctrl"
                 (fn [msg] (dispatch! (st/msg->key msg)))))

