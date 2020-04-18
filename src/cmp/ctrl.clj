(ns cmp.ctrl
  ^{:author "wactbprot"
    :doc "Observes the `ctrl` interface."}
  (:require [taoensso.timbre :as timbre]
            [cmp.st-mem :as st]
            [clojure.core.async :as a]
            [cmp.excep :as excep]
            [cmp.state :as state]
            [cmp.utils :as u]))

;;------------------------------
;; ctrl-dispatch!
;;------------------------------
(defn dispatch
  "Dispatches on the value of the
  `ctrl` interface  for the structure
  belonging to `k`."
  [k]
  (timbre/info "ctrl dispatch call for path: " k)
  (if k
    (let [cmd (->> k
                   st/key->val
                   u/get-next-ctrl)]
      (a/>!! state/ctrl-chan [k cmd]))))

;;------------------------------
;; stop
;;------------------------------
(defn stop
  "De-registers the listener for the `ctrl`
  interfacees of the `mp-id`. After stopping
  the system will no longer react on changes
  at the `ctrl` interface."
  [mp-id]
  (st/de-register! mp-id "*" "*" "ctrl"))

;;------------------------------
;; start
;;------------------------------
(defn start
  "Registers a listener for the `ctrl` interface of
  the entire `mp-id`. The [[dispatch]] function
  becomes the listeners `callback`." 
  [mp-id]
  (timbre/info "register ctrl listener for: " mp-id)
  (let [callback (fn [msg]
                   (dispatch
                    (st/msg->key msg)))]
    (st/register! mp-id "*" "*" "ctrl" callback)))
