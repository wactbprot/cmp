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
            [cmp.run :as run]
            [cmp.utils :as u]))

(def heartbeat 1000)
(def mon (atom {}))

;;------------------------------
;; exception channel 
;;------------------------------
(def excep-chan (a/chan))
(a/go
  (while true
    (let [e (a/<! excep-chan)] 
      (timbre/error (.getMessage e)))))

;;------------------------------
;; register
;;------------------------------
(defn register!
  "Sets the `mon` atom `true` for the path `p`
  so that the [[monitor]]s `while` continues."
  [p]
  (timbre/debug "register channel for path: " p)
  (swap! mon assoc p true))

(defn de-register!
  "Sets the `mon` atom `false` for the path `p`
  so that the [[monitor]]s `while` stops."
  [p]
  (timbre/debug "de-register channel for path: " p)
  (swap! mon assoc p false))

(defn registered?
  "Returns the state of the atom `mon`
  for the given path `p`."
  [p]
  (timbre/debug "registered path: " p)
  ((deref mon) p))

;;------------------------------
;; dispatch
;;------------------------------
(defmulti dispatch
  (fn [ctrl-str ctrl-path]
    (keyword (u/get-next-ctrl ctrl-str))))

(defmethod dispatch :run
  [ctrl-str ctrl-path]
  (timbre/debug "dispatch run branch for key: " ctrl-path)
  (st/set-val! ctrl-path "running")
  (a/>!! run/ctrl-chan ctrl-path))

(defmethod dispatch :running
  [ctrl-str ctrl-path]
  (timbre/debug "dispatch running branch for key: " ctrl-path)
  (a/>!! run/ctrl-chan ctrl-path))

(defmethod dispatch :default
  [ctrl-str ctrl-path]
  (timbre/warn "dispatch default branch for key: " ctrl-path))

;;------------------------------
;; monitor
;;------------------------------
(defn monitor!
  [p]
  (a/go
    (while ((deref mon) p)
      (a/<! (a/timeout heartbeat))
      (try
        (dispatch (st/key->val p) p)
        (catch Exception e
          (timbre/error "catch error at channel " p)
          (a/>! excep-chan e))))))

;;------------------------------
;; start
;;------------------------------
(defn start
  "Registers and monitors the the struct
  (`container` or `definitions`) belonging to path `p`.
  `p` is a string of the form `se3-calib@container@1@ctrl`.
  The `registered?` predicate function avoids starting more
  than one `go-loop` for one path `p`."
  [p]
  (let [reg (registered? p)]
    (cond
      (true? reg) (timbre/warn "already monitoring" p)
      (or
       (nil? reg)
       (false? reg)) (do
                       (timbre/debug "register and start monitoring" p)
                       (register! p)
                       (monitor! p)))))

;;------------------------------
;; stop
;;------------------------------
(defn stop
  "Stop the monitoring of the struct
  (`container` or `definitions`)  by de-registering it." 
  [p]
  (de-register! p)
  (timbre/debug "close monitor channel registered for path: " p))
