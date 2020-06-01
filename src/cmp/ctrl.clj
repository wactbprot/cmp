(ns cmp.ctrl
  ^{:author "wactbprot"
    :doc "Observes the `ctrl` interface."}
  (:require [taoensso.timbre :as log]
            [cmp.st-mem :as st]
            [cmp.state :as state]
            [cmp.utils :as u]))

;;------------------------------
;; ctrl-dispatch
;;------------------------------
(defn dispatch
  "Dispatches on the value of the
  `ctrl` interface  for the structure
  belonging to `k`."
  [k]
  (log/info "ctrl dispatch call for path: " k)
  (when k
    (let [cmd (u/get-next-ctrl (st/key->val k))]
      (state/dispatch k cmd))))

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
  becomes the listeners `cb!`." 
  [mp-id]
  (log/info "register ctrl listener for: " mp-id)
  (let [cb! (fn [msg]
                   (dispatch
                    (st/msg->key msg)))]
   (st/register! mp-id "*" "*" "ctrl" cb!)))
