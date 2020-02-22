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
  belonging to `p`."
  [p]
  (let [cmd (->> p
                 (st/key->val)
                 (u/get-next-ctrl))]
    (a/>!! state/ctrl-chan [p cmd])))

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
  (st/register! mp-id "*" "*" "ctrl"
                (fn [msg] (dispatch (st/msg->key msg)))))

;;------------------------------
;; ctrl channel invoked by run 
;;------------------------------
(def ctrl-chan (a/chan))

;;------------------------------
;; ctrl go block 
;;------------------------------
(a/go
  (while true  
    (let [[mp-id cmd] (a/<! ctrl-chan)]
      (try
        (timbre/info "receive key " mp-id "and" cmd)
        (condp = (keyword cmd)
          :start  (start mp-id)
          :stop   (stop mp-id)
          (timbre/error "no case for " mp-id "and" cmd))
        (catch Exception e
          (timbre/error "catch error at channel " mp-id)
          (a/>! excep/ch e))))))