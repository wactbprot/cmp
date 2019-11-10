(ns cmp.core
  (:require [cmp.lt :as lt]
            [cmp.st :as st]
            [cmp.utils :as u]
            [cmp.doc :as d]
            [cmp.build :as b]
            [cmp.check :as check]
            [cmp.poll :as poll]
            [cmp.log :as log]
            [taoensso.timbre :as timbre]
            [cmp.config :as cfg]
            )
  (:gen-class)
  (:use [clojure.repl]))

;;------------------------------
;; log
;;------------------------------
;; (log/init)
;; (log/stop-repl-out)
;; (log/start-repl-out)

(def current-mp-id (atom nil))

(defn workon
  [mp-id]
  (reset! current-mp-id mp-id))

(defn ->mp-id
  []
  (if-let [mp-id (deref current-mp-id)]
    mp-id
    (throw (Exception. "No mp-id set.\n\n\nUse function (workon <mp-id>)"))))

;;------------------------------
;; build
;;------------------------------
(defn build
  "Loads document from long term memory and
  fetches it to short term memory"
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
  "Adds a doc to the api to store the resuls in."
  [doc-id]
  (d/add (u/extr-main-path (->mp-id)) doc-id))

(defn doc-del
  "Removes a doc from the api."
  [doc-id]
  (d/del (u/extr-main-path (->mp-id)) doc-id))

;;------------------------------
;; check mp tasks
;;------------------------------
(defn check
  "Check and runs the tasks of the container and definitions"
  []
  (timbre/info "check : " (->mp-id))
  (let [p (u/extr-main-path (->mp-id))
        n-cont (st/val->int (st/get-val (u/get-meta-ncont-path p)))
        n-defins (st/val->int (st/get-val (u/get-meta-ndefins-path p)))]
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
  "Check and runs the tasks of the containers and definitions."
  []
  (timbre/info "start polling for: " (->mp-id))
  (let [p (u/extr-main-path (->mp-id))
        n-cont (st/val->int (st/get-val (u/get-meta-ncont-path p)))
        n-defins (st/val->int (st/get-val (u/get-meta-ndefins-path p)))]
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
  "Check and runs the tasks of the containers and definitions."
  []
  (timbre/info "stop polling of " (->mp-id))  
  (let [p (u/extr-main-path (->mp-id))
        n-cont (st/val->int (st/get-val (u/get-meta-ncont-path p)))
        n-defins (st/val->int (st/get-val (u/get-meta-ndefins-path p)))]
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
  The mp-id is received over (->mp-id). Defins should not be
  started by user"
  [i cmd]
  (timbre/info "push cmd to:" (->mp-id))
  (let [p (u/get-cont-ctrl-path (u/extr-main-path (->mp-id)) i)]
    (st/set-val! p cmd))
  (timbre/info "done  [" (->mp-id) "]" ))
 