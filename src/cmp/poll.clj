(ns cmp.poll
  ^{:author "wactbprot"
    :doc "Polls the short term memory endpoint `ctrl` 
          and dispatchs depending on the result 
          (`:load`, `:run`, `:stop` etc)."}
  (:require [clojure.string :as string]
            [taoensso.timbre :as timbre]
            [clojure.core.async :as a]
            [cmp.st-mem :as st]
            [cmp.check :as chk]
            [cmp.config :as cfg]
            [cmp.run :as run]
            [cmp.utils :as u]))


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
(defmulti dispatch
  (fn
    [ctrl-str ctrl-path]
    (u/get-next-ctrl ctrl-str)))

(defmethod dispatch :run
  [ctrl-str ctrl-path]
  (timbre/debug "dispatch run for key: " ctrl-path)
  (a/>!! run/ctrl-chan ctrl-path))

(defmethod dispatch :mon
  [ctrl-str ctrl-path]
  (timbre/debug "dispatch run for key: " ctrl-path)
  (a/>!! run/ctrl-chan ctrl-path))

(defmethod dispatch :suspend
  [ctrl-str ctrl-path]
  (timbre/debug "suspend for key: " ctrl-path))

(defmethod dispatch :default
  [ctrl-str ctrl-path]
  (timbre/debug "dispatch default branch for key: " ctrl-path))

;;------------------------------
;; continue monitoring
;;------------------------------
(defn cont-mon?
  "The string `\"stop\"` stops the polling

  **TODO:** explicit doc tests"
  ([]
   false)
  ([ctrl-str]
   (not=
    :stop
    (u/get-next-ctrl ctrl-str))))

;;------------------------------
;; monitor
;;------------------------------
(def heartbeat
  "The `heartbeat` of the cmp taken from [[../config.edn]]."
  (cfg/heartbeat (cfg/config)))
(defn monitor!
  "Polls the `ctrl-str` at path `p` and dispatches
  the resulting `ctrl-cmd`."
  [p]
  (a/go
    (while (cont-mon? (st/key->val p))
      (a/<! (a/timeout heartbeat))
      (try
        (dispatch (st/key->val p) p)
        (catch Exception e
          (timbre/error "catch error at channel " p)
          (a/>! excep-chan e))))))
