(ns cmp.core
  ^{:author "wactbprot"
    :doc "Provides the api of cmp."}
  (:require [cmp.lt :as lt]
            [cmp.st :as st]
            [cmp.utils :as utils]
            [cmp.doc :as doc]
            [cmp.build :as build]
            [cmp.check :as check]
            [cmp.poll :as poll]
            [cmp.run :as run]
            [cmp.log :as log]
            [taoensso.timbre :as timbre])
  (:gen-class)
  (:use [clojure.repl]))

;;------------------------------
;; log
;;------------------------------
(defn log-init!
  "Initializes a `std-out` on `info` level and a
  `gelf` appender on `debug` level."
  []
  (log/init))

(defn log-stop-repl-out!
  "Stops the println appender."
  []
  (log/stop-repl-out))

(defn log-start-repl-out!
  "Starts the println appender."
  []
  (log/start-repl-out))


;;------------------------------
;; current-mp-id atom and workon
;;------------------------------
(def current-mp-id
  "Provides a storing place for the current mp-id
  for convenience. Due to this atom the `(build)`,
  `(check)` or `(start)` function needs no argument." 
  (atom nil))

(defn workon
  "Sets the mpd to work on (see [[current-mp-id]]).

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
  builds the short term memory. The `mp-id`
  must be set with [[workon]].  
  
  Usage:
  
  ```clojure
  (build mpid)
  ;; or
  (workon mpid)
  ;; followed by
  (build)
  (check)
  (start)
  ```"
  ([]
   (build (->mp-id)))
  ([mp-id]
   (timbre/info "build " mp-id)
   (build/store (lt/get-doc (utils/compl-main-path mp-id)))
   (timbre/info "done  [" mp-id "]" )))

;;------------------------------
;; clear
;;------------------------------
(defn clear
  "Clears all short term memory for the given `mp-id`
  (see [[workon]]).
   Usage:
  
  ```clojure
  (clear mpid)
  ;; or
  (workon mpid)
  (clear)
  
  ```"
  ([]
   (clear (->mp-id)))
  ([mp-id]
   (timbre/info "clear " mp-id )
   (st/clear (utils/extr-main-path mp-id))
   (timbre/info "done  [" mp-id "]" )))

;;------------------------------
;; documents
;;------------------------------
(defn doc-add
  "Adds a doc to the api to store the resuls in. (untested)"
  [doc-id]
  (doc/add (utils/extr-main-path (->mp-id)) doc-id))

(defn doc-del
  "Removes a doc from the api. (untested)"
  [doc-id]
  (doc/del (utils/extr-main-path (->mp-id)) doc-id))

;;------------------------------
;; check mp tasks
;;------------------------------
(defn check
  "Check the tasks of the `container` and
  `definitions` structure. "
  ([]
   (check (->mp-id)))
  ([mp-id]
   (timbre/info "check: " mp-id)
   (let [p (utils/extr-main-path mp-id)
         k-ncont (utils/get-meta-ncont-path p)
         n-cont (utils/val->int (st/key->val k-ncont))
         k-ndefins (utils/get-meta-ndefins-path p)
         n-defins (utils/val->int (st/key->val k-ndefins))]
     (run!
      (fn [i]
        (check/struct-tasks (utils/get-cont-defin-path p i)))
      (range n-cont))
     (run!
      (fn [i]
        (check/struct-tasks (utils/get-defins-defin-path p i)))
      (range n-defins)))
   (timbre/info "done  [" mp-id "]" )))

;;------------------------------
;; start polling
;;------------------------------
(defn start
  "Check and runs the tasks of the containers and definitions.
  (see [[workon]])."
  ([]
   (start (->mp-id)))
  ([mp-id]
   (timbre/info "start polling for: " mp-id)
   (let [p (utils/extr-main-path mp-id)
         n-cont (utils/val->int (st/key->val (utils/get-meta-ncont-path p)))
         n-defins (utils/val->int (st/key->val (utils/get-meta-ndefins-path p)))]
     (run!
      (fn [i]
        (poll/start (utils/get-cont-ctrl-path p i)))
      (range n-cont))
     (run!
      (fn [i]
        (poll/start (utils/get-defins-ctrl-path p i)))
     (range n-defins)))
   (timbre/info "done  [" mp-id "]" )))
  
;;------------------------------
;; stop all polling
;;------------------------------
(defn stop
  "Check and runs the tasks of the containers and definitions
  (see [[workon]])."
  ([]
   (stop (->mp-id)))
  ([mp-id]
  (timbre/info "stop polling of " mp-id)  
  (let [p (utils/extr-main-path mp-id)
        n-cont (utils/val->int (st/key->val (utils/get-meta-ncont-path p)))
        n-defins (utils/val->int (st/key->val (utils/get-meta-ndefins-path p)))]
    (run!
     (fn [i]
       (poll/stop (utils/get-cont-ctrl-path p i)))
     (range n-cont))
    (run!
     (fn [i]
       (poll/stop (utils/get-defins-ctrl-path p i)))
     (range n-defins)))
  (timbre/info "done  [" mp-id "]" )))

;;------------------------------
;; push ctrl commands
;;------------------------------
  
(defn push
  "push a cmd string to the control interface of a mp.
  The mp-id is received over (->mp-id). The defins
  struct should not be started by user
  (see [[workon]])."
  ([i cmd]
   (push (->mp-id) i cmd))
  ([mp-id i cmd]
  (timbre/info "push cmd to:" mp-id)
  (let [p (utils/get-cont-ctrl-path (utils/extr-main-path mp-id) i)]
    (st/set-val! p cmd))
  (timbre/info "done  [" mp-id "]" )))

;;------------------------------
;; poll status
;;------------------------------
(defn poll-status
  []
  (doseq [[k v] (deref poll/mon)]
    (utils/print-kv k v)))

;;------------------------------
;; cont status
;;------------------------------
(defn cont-status
  ([i]
   (cont-status (->mp-id) i))
  ([mp-id i]
   (run/status
    (utils/get-cont-ctrl-path
     (utils/extr-main-path mp-id) i))))
