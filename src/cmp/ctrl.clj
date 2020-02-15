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
;; ctrl channel invoked by run 
;;------------------------------
(def ctrl-chan (a/chan))

;;------------------------------
;; ctrl-dispatch!, start, stop
;;------------------------------
(defn stop
  "De-registers a listener for the `ctrl`
  interface of the `mp-id`."
  [mp-id]
  (st/de-register! mp-id "*" "*" "ctrl"))

(defn dispatch!
  "Dispatches on the value of the
  `ctrl` interface `p`."
  [p]
  (let [mp-id (u/key->mp-name p)
        cmd (->> p
                 (st/key->val)
                 (u/get-next-ctrl)
                 keyword)]
    (condp = cmd
      :run     (a/>!! state/ctrl-chan [p :start])
      :mon     (a/>!! state/ctrl-chan [p :start])
      :suspend (a/>!! state/ctrl-chan [p :stop])
      :stop    (do
                 (a/>!! state/ctrl-chan [p :stop])
                 (stop mp-id))
      (timbre/debug "dispatch default branch for key: " p))))

(defn start
  "Registers a listener for the `ctrl` interface of the `mp-id`.
  The [[dispatch]] function becomes the `callback`." 
  [mp-id]
  (st/register! mp-id "*" "*" "ctrl"
                 (fn [msg] (dispatch! (st/msg->key msg)))))

;;------------------------------
;; ctrl go block 
;;------------------------------
(a/go
  (while true  
    (let [[k cmd] (a/<! ctrl-chan)]
      (try
        (timbre/debug "receive key " k "and" cmd)            
        (condp = cmd
          :start (start k)
          :stop (stop k))
        (catch Exception e
          (timbre/error "catch error at channel " k)
          (a/>! excep/ch e))))))
