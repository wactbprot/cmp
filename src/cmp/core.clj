(ns cmp.core
  (:require [cmp.lt :as lt]
            [cmp.st :as st]
            [cmp.utils :as u]
            [cmp.doc :as d]
            [cmp.build :as b]
            [cmp.check :as check]
            [cmp.poll :as poll]
            [cmp.run :as run]
            [cmp.log :as log]
            [taoensso.timbre :as timbre]
            [cmp.config :as cfg]
            )
  (:gen-class)
  (:use [clojure.repl]))

;;------------------------------
;; log
;;------------------------------
(defn log-init!
  []
  log/init)

(defn log-stop-repl-out!
  []
  (log/stop-repl-out))

(defn log-start-repl-out!
  []
  (log/start-repl-out))


;;------------------------------
;; current-mp-id atom and workon
;;------------------------------
(def current-mp-id (atom nil))

(defn workon
  "Sets the mpd to workon.

  Usage:
  
  ```clojure
  (workon 'se3-calib')
  (->mp-id)
  ```
  "
  [mp-id]
  (reset! current-mp-id mp-id))

(defn ->mp-id
  "Returns the mpd-id set with workon.

  Usage:
  
  ```clojure
  (workon 'se3-calib')
  (->mp-id)
  ```
  "
  []
  (if-let [mp-id (deref current-mp-id)]
    mp-id
    (throw (Exception. "No mp-id set.\n\n\nUse function (workon <mp-id>)"))))

;;------------------------------
;; build
;;------------------------------
(defn build
  "Loads mpd from long term memory and
  builds the short term memory
  
  Usage:
  
  ```clojure
  (build)
  ```"
  []
  (timbre/info "build " (->mp-id) )
  (b/store (lt/get-doc (u/compl-main-path (->mp-id))))
  (timbre/info "done  [" (->mp-id) "]" ))

;;------------------------------
;; clear
;;------------------------------
(defn clear
  "Clears all short term memory for the given mp-id"
  []
  (timbre/info "clear " (->mp-id) )
  (st/clear (u/extr-main-path (->mp-id)))
  (timbre/info "done  [" (->mp-id) "]" ))

;;------------------------------
;; documents
;;------------------------------
(defn doc-add
  "Adds a doc to the api to store the resuls in.
  Only works in combination with [[workon]]."
  [doc-id]
  (d/add (u/extr-main-path (->mp-id)) doc-id))

(defn doc-del
  "Removes a doc from the api.
  Only works in combination with [[workon]]."
  [doc-id]
  (d/del (u/extr-main-path (->mp-id)) doc-id))

;;------------------------------
;; check mp tasks
;;------------------------------
(defn check
  "Check the tasks of the container and definitions.
  Only works in combination with [[workon]]."
  []
  (timbre/info "check: " (->mp-id))
  (let [p (u/extr-main-path (->mp-id))
        n-cont (u/val->int (st/get-val (u/get-meta-ncont-path p)))
        n-defins (u/val->int (st/get-val (u/get-meta-ndefins-path p)))]
    (run!
     (fn [i]
       (check/struct (u/get-cont-defin-path p i)))
     (range n-cont))
    (run!
     (fn [i]
       (check/struct (u/get-defins-defin-path p i)))
     (range n-defins)))
  (timbre/info "done  [" (->mp-id) "]" ))

;;------------------------------
;; start polling
;;------------------------------
(defn start
  "Check and runs the tasks of the containers and definitions.
  Only works in combination with [[workon]]."
  []
  (timbre/info "start polling for: " (->mp-id))
  (let [p (u/extr-main-path (->mp-id))
        n-cont (u/val->int (st/get-val (u/get-meta-ncont-path p)))
        n-defins (u/val->int (st/get-val (u/get-meta-ndefins-path p)))]
    (run!
     (fn [i]
       (poll/start (u/get-cont-ctrl-path p i)))
     (range n-cont))
    (run!
     (fn [i]
       (poll/start (u/get-defins-ctrl-path p i)))
     (range n-defins)))
  (timbre/info "done  [" (->mp-id) "]" ))

;;------------------------------
;; stop all polling
;;------------------------------
(defn stop
  "Check and runs the tasks of the containers and definitions.
  Only works in combination with [[workon]]."
  []
  (timbre/info "stop polling of " (->mp-id))  
  (let [p (u/extr-main-path (->mp-id))
        n-cont (u/val->int (st/get-val (u/get-meta-ncont-path p)))
        n-defins (u/val->int (st/get-val (u/get-meta-ndefins-path p)))]
    (run!
     (fn [i]
       (poll/stop (u/get-cont-ctrl-path p i)))
     (range n-cont))
    (run!
     (fn [i]
       (poll/stop (u/get-defins-ctrl-path p i)))
     (range n-defins)))
  (timbre/info "done  [" (->mp-id) "]" ))

;;------------------------------
;; push ctrl commands
;;------------------------------
  
(defn push
  "push a cmd string to the control interface of a mp.
  The mp-id is received over (->mp-id). The defins
  struct should not be started by user.
  Only works in combination with [[workon]]."
  [i cmd]
  (timbre/info "push cmd to:" (->mp-id))
  (let [p (u/get-cont-ctrl-path (u/extr-main-path (->mp-id)) i)]
    (st/set-val! p cmd))
  (timbre/info "done  [" (->mp-id) "]" ))

;;------------------------------
;; poll status
;;------------------------------
(defn poll-status
  []
  (doseq [[k v] (deref poll/mon)]
    (u/print-kv k v)))


;;------------------------------
;; cont status
;;------------------------------
(defn cont-status
  [i]
  (run/status
   (u/get-cont-ctrl-path
    (u/extr-main-path (->mp-id)) i)))
