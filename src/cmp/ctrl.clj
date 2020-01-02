(ns cmp.ctrl
  ^{:author "wactbprot"
    :doc "Observes the ctrl interface."}
  (:require [taoensso.timbre :as timbre]
            [clojure.core.async :as a]
            [cmp.st-mem :as st]
            [cmp.reg :as reg]
            [cmp.state :as state]
            [cmp.utils :as u]))

;;------------------------------
;; ctrl-dispatch!, start, stop
;;------------------------------
(defn stop
  [mp-id]
  (reg/de-register! mp-id "ctrl"))

(defn ctrl-dispatch!
  "Dispatches on the ctrl path `p`"
  [p]
  (let [mp-id (u/key->mp-name p)
        cmd (->> p
                  (st/key->val)
                  (u/get-next-ctrl))]
    (timbre/debug "dispatch for key: " p " and cmd: " cmd)
    (cond
      (= cmd "run") (state/start p)
      (= cmd "mon") (state/start p)
      (= cmd "suspend") (state/stop p)
      (= cmd "stop") (do
                      (state/stop p)
                      (stop mp-id))
      :default (timbre/debug "dispatch default branch for key: " p))))

(defn start
  [mp-id]
  (reg/register! mp-id "ctrl" (fn [msg]
                            (ctrl-dispatch! (st/msg->key msg)))))

