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
            [cmp.config :as cfg]
            [cmp.build :as bld]
            [cmp.check :as chk]
            [cmp.ctrl :as ctrl]
            [cmp.state :as state]
            [cmp.log :as log]
            [taoensso.timbre :as timbre]))

;;------------------------------
;; log
;;------------------------------
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
;; build-mpd
;;------------------------------
(defn build-mpd
  "Loads mpd from long term memory and
  builds the short term memory. The `mp-id`
  must be set with [[workon!]].  
  
  Usage:
  
  ```clojure
  (build-mpd mpid)
  ;; or
  (workon! mpid)
  ;; followed by
  (build-mpd)
  (check)
  (start)
  ```"
  ([]
   (build-mpd (->mp-id)))
  ([mp-id]
   (timbre/info "build " mp-id)
   (->> mp-id
        (u/compl-main-path)
        (lt/id->doc)
        (u/doc->safe-doc)
        (bld/store))))

(defn build-mpd-edn
  "Builds up a mp from the `edn`.
  
  ```clojure
  (build-mpd-edn \"resources/mpd-modbus.edn\")
  ;; (\"OK\" \"OK\" \"OK\")
  ```
  "
  []
  (run!
   (fn [uri]
     (timbre/info "try to slurp and build: " uri  )
     (bld/store
      (read-string
       (slurp uri))))
   (cfg/edn-mpds (cfg/config))))

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
   (let [p         (u/extr-main-path mp-id)
         k-ncont   (u/get-meta-ncont-path p)
         n-cont    (st/key->val k-ncont)
         k-ndefins (u/get-meta-ndefins-path p)
         n-defins  (st/key->val k-ndefins)]
     (run!
      (fn [i]
        (chk/struct-tasks (u/get-cont-defin-path p i)))
      (range n-cont))
     (run!
      (fn [i]
        (chk/struct-tasks (u/get-defins-defin-path p i)))
      (range n-defins)))))

;;------------------------------
;; start observing
;;------------------------------
(defn start-observe
  "Registers a listener for the `ctrl`
  interface of a `mp-id` (see [[workon!]])."
  ([]
   (start-observe (->mp-id)))
  ([mp-id]
   (a/>!! ctrl/ctrl-chan [mp-id :start])
   (timbre/info "sent register request for: " mp-id)))

;;------------------------------
;; stop observing 
;;------------------------------
(defn stop-observe
  "De-registers the listener for the `ctrl`
  interface of the given `mp-id` (see [[workon!]])."
  ([]
   (stop-observe (->mp-id)))
  ([mp-id]
   (a/>!! ctrl/ctrl-chan [mp-id :stop])
   (timbre/info "sent de-register request for: " mp-id)))

;;------------------------------
;; push ctrl commands
;;------------------------------
(defn set-ctrl
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
   (set-ctrl (->mp-id) i cmd))
  ([mp-id i cmd]
   (let [p (u/get-cont-ctrl-path (u/extr-main-path mp-id) i)]
     (st/set-val! p cmd))))

(defn run-c
  "Shortcut to push a `run` to the control
  interface of  mp container `i`."
  [i]
  (set-ctrl (->mp-id) i "run"))


(defn stop-c
  "Shortcut to push a `stop` to the control
  interface of  mp container `i`."
  [i]
  (set-ctrl (->mp-id) i "stop"))

(defn reset-c
  "Shortcut to push a `reset` to the control
  interface of  mp container `i`. The `reset` cmd
  does **not** de-register the state listener so
  that the container starts from the beginning.
  **reset is a container restart**
  "
  [i]
  (set-ctrl (->mp-id) i "reset"))

;;------------------------------
;; status (stat)
;;------------------------------
(defn stat-c
  "Returns the  **c**ontainer **s**tatus.
  Returns the state map for the `i` container."
  ([i]
   (stat-c (->mp-id) i))
  ([mp-id i]
   (state/cont-status mp-id i)))

(defn stat-d
  "Returns  **d**efinitions **s**tatus.
  Returns the `state map` for the `i`
  definitions structure."
  ([i]
   (stat-d (->mp-id) i))
  ([mp-id i]
   (state/defins-status mp-id i)))


;;------------------------------
;; tasks
;;------------------------------
(defn build-tasks
  "Builds the `tasks` endpoint. At
  runtime all `tasks` are provided by
  `st-mem`" 
  []
  (bld/store-tasks (lt/get-all-tasks)))


(defn build-task-edn
  "Stores the `task` slurping from the files
  given in `resources/config.edn`

  Usage:
  
  ```clojure
  (build-tasks-edn)
  ```"
  []
  (run!
   (fn [uri]
     (timbre/info "try to slurp and build: " uri  )
       (bld/store-task
        (read-string
         (slurp uri))))
     (cfg/edn-tasks (cfg/config))))

(defn refresh-tasks
  "Refreshs the `tasks` endpoint.
  
  Usage:
  
  ```clojure
  (refresh-tasks)
  ```
  "
  []
  (timbre/info "clear tasks")
  (bld/clear-tasks)
  (timbre/info "build tasks from db")
  (bld/store-tasks (lt/get-all-tasks))
  (timbre/info "build edn tasks")
  (build-task-edn))


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
   (stop-observe mp-id)
   (st/clear (u/extr-main-path mp-id))))


(defn clear-all
  "The pattern `*@meta@name` is used to find all
   mp-names. Function removes all keys of all `mpd`s"  
  []
  (clear "task")
  (map clear
       (map u/key->mp-name
            (st/pat->keys "*@meta@name"))))
