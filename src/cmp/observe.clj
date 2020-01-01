(ns cmp.observe
  ^{:author "wactbprot"
    :doc "Register ans deregister listeners for the 
          short term memory endpoint `ctrl` 
          and dispatchs depending on the result 
          (`:load`, `:run`, `:stop` etc)."}
  (:require [taoensso.timbre :as timbre]
            [clojure.core.async :as a]
            [cmp.st-mem :as st]
            [cmp.config :as cfg]
            [cmp.run :as run]
            [cmp.utils :as u]))

;;------------------------------
;; listeners
;;------------------------------
(def listeners (atom {}))

;;------------------------------
;; exception channel 
;;------------------------------
(def excep-chan (a/chan))
(a/go
  (while true
    (let [e (a/<! excep-chan)] 
      (timbre/error (.getMessage e)))))

;;------------------------------
;; dispatch
;;------------------------------
(defn dispatch
  "Dispatches on the ctrl path `p`"
  [p]
  (let [ cmd (->> p
                  (st/key->val)
                  (u/get-next-ctrl)
                  (keyword)) ]
    (timbre/debug "dispatch for key: " p " and cmd: " cmd)
    (cond
      (= cmd :run) (a/>!! run/ctrl-chan p)
      (= cmd :mon) (a/>!! run/ctrl-chan p)
      (= cmd :suspend) (timbre/debug "suspend for key: " p)
      :default (timbre/debug "dispatch default branch for key: " p))))

(defn registered?
  "Checks if a `listener` is registered under
  `listeners`-atom"
  [mp-id]
  (contains? (deref listeners) mp-id))

(defn register!
  "Generates a `ctrl` listener and registers him
  under the key `mp-id` in the `listeners` atom."
  [mp-id]
  (cond
    (registered? mp-id) (timbre/info "a ctrl listener for "
                                     mp-id
                                     " is already registered") 
    :else (swap! listeners  assoc
                 mp-id
                 (st/gen-listener mp-id "ctrl"
                                  (fn
                                    [msg]
                                    (dispatch (st/msg->key msg)))))))

(defn de-register!
  "De-registers the listener with the
  key `mp-id` in the `listeners` atom."
  [mp-id]
  (cond
    (registered? mp-id) (do
            (st/close-listener! ((deref listeners) mp-id))
            (swap! listeners dissoc mp-id))
    :else (timbre/info "a ctrl listener for "
                       mp-id
                       " is not registered")))
   