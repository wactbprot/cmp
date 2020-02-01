(ns cmp.core
  ^{:author "wactbprot"
    :doc "Provides the api of cmp. `(start)`, `(stop)` etc. 
          are intended for **repl** use only. Graphical user 
          interfaces should attache to the **short term memory**."}
  (:require [cmp.lt-mem :as lt]
            [cmp.st-mem :as st]
            [clojure.core.async :as a]
            [cmp.utils :as u]
            [cmp.doc :as doc]
            [cmp.build :as bld]
            [cmp.check :as chk]
            [cmp.ctrl :as ctrl]
            [cmp.state :as state]
            [cmp.log :as log]
            [taoensso.timbre :as timbre])
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

(defn workon!
  "Sets the mpd to work on (see [[current-mp-id]]).

  Usage:
  
  ```clojure
  (workon! 'se3-calib')
  (->mp-id)
  ```
  "
  [mp-id]
  (reset! current-mp-id mp-id))

(defn ->mp-id
  "Returns the mpd-id set with workon!.

  Usage:
  
  ```clojure
  (workon! 'se3-calib')
  (->mp-id)
  ```
  "
  []
  (if-let [mp-id (deref current-mp-id)]
    mp-id
    (throw (Exception. "No mp-id set.\n\n\nUse function (workon! <mp-id>)"))))

;;------------------------------
;; build
;;------------------------------
(defn build
  "Loads mpd from long term memory and
  builds the short term memory. The `mp-id`
  must be set with [[workon!]].  
  
  Usage:
  
  ```clojure
  (build mpid)
  ;; or
  (workon! mpid)
  ;; followed by
  (build)
  (check)
  (start)
  ```"
  ([]
   (build (->mp-id)))
  ([mp-id]
   (timbre/info "build " mp-id)
   (->> mp-id
        (u/compl-main-path)
        (lt/id->doc)
        (u/doc->safe-doc)
        (bld/store))
   (timbre/info "done  [" mp-id "]" )))

(defn build-edn
  "Builds up a mp from the `edn`.
  
  ```clojure
  (build-edn \"resources/mpd-modbus.edn\")
  ;; (\"OK\" \"OK\" \"OK\")
  ```
  "
  [uri]
  (bld/store
   (read-string
    (slurp uri))))

(defn build-ref
  "Builds up a reference or example structure
  for testing and documentation 
  (`./recources/mpd-ref.edn`).
  
  ```clojure
  (build-ref)
  ;; (\"OK\" \"OK\" \"OK\")
  ;;
  ;; complete file:
  (read-string
   (slurp \"resources/ref-mpd.edn\"))
  ```
  "
  []
  (build-edn "resources/ref-mpd.edn"))

;;------------------------------
;; documents
;;------------------------------
(defn doc-add
  "Adds a doc to the api to store the resuls in. (untested)"
  [doc-id]
  (doc/add (u/extr-main-path (->mp-id)) doc-id))

(defn doc-del
  "Removes a doc from the api. (untested)"
  [doc-id]
  (doc/del (u/extr-main-path (->mp-id)) doc-id))

;;------------------------------
;; check mp tasks
;;------------------------------
(defn check
  "Check the tasks of the `container` and
  `definitions` structure. *cmp* does not preload
  the tasks, they are loaded from the `lt-mem`
  during runtime."
  ([]
   (check (->mp-id)))
  ([mp-id]
   (timbre/info "check: " mp-id)
   (let [p         (u/extr-main-path mp-id)
         k-ncont   (u/get-meta-ncont-path p)
         n-cont    (u/val->int (st/key->val k-ncont))
         k-ndefins (u/get-meta-ndefins-path p)
         n-defins  (u/val->int (st/key->val k-ndefins))]
     (run!
      (fn [i]
        (chk/struct-tasks (u/get-cont-defin-path p i)))
      (range n-cont))
     (run!
      (fn [i]
        (chk/struct-tasks (u/get-defins-defin-path p i)))
      (range n-defins)))
   (timbre/info "done  [" mp-id "]" )))

;;------------------------------
;; start
;;------------------------------
(defn start
  "Registers a listener for the `ctrl` interface.
  (see [[workon!]])."
  ([]
   (start (->mp-id)))
  ([mp-id]
   (timbre/info "register observer for: " mp-id)
   (a/>!! ctrl/ctrl-chan [mp-id :start])))
  

;;------------------------------
;; stop
;;------------------------------
(defn stop
  "Registers a listener for the `ctrl` interface.
  (see [[workon!]])."
  ([]
   (stop (->mp-id)))
  ([mp-id]
   (timbre/info "stop observing " mp-id)
   (a/>!! ctrl/ctrl-chan [mp-id :stop])))

;;------------------------------
;; push ctrl commands
;;------------------------------
(defn ctrl!
  "Push a command string (`cmd`) to the control
  interface of a mp. `cmd`s are:
  
  * `\"run\"`
  * `\"stop\"`
  * `\"mon\"`
  * `\"suspend\"`

  The `mp-id` is received over `(->mp-id)`.
  **NOTE:** The `definitions` struct should not
  be started by user (see [[workon!]])."
  ([i cmd]
   (ctrl! (->mp-id) i cmd))
  ([mp-id i cmd]
   (timbre/info "push cmd to:" mp-id)
   (let [p (u/get-cont-ctrl-path (u/extr-main-path mp-id) i)]
     (st/set-val! p cmd))
   (timbre/info "done  [" mp-id "]" )))

;;------------------------------
;; clear
;;------------------------------
(defn clear
  "Clears all short term memory for the given `mp-id`
  (see [[workon!]]).
   Usage:
  
  ```clojure
  (clear mpid)
  ;; or
  (workon! mpid)
  (clear)
  
  ```"
  ([]
   (clear (->mp-id)))
  ([mp-id]
   (stop mp-id)
   (timbre/info "clear " mp-id )
   (st/clear (u/extr-main-path mp-id))
   (timbre/info "done  [" mp-id "]" )))

(defn status
  ([no]
   (status (->mp-id) no))
  ([mp-id no]
   (state/status mp-id no)))


(defn workon!!
  "Sets the mpd to work on, then starts:
  `(clear)`, `(build)`, `(check)`  and `(start)`
  
  Usage:
  
  ```clojure
  (workon!! 'se3-calib')
  ```
  "
  [mp-id]
  (workon! mp-id)
  (clear)
  (build)
  (check)
  (start))
