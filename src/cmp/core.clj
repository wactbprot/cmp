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
  
  Example:
  ```clojure
  (workon! 'se3-calib')
  (->mp-id)
  ```"
  []
  (if-let [mp-id (deref current-mp-id)]
    mp-id
    (throw (Exception. "No mp-id set.\n\n\nUse the function (workon! <mp-id>)"))))


;;------------------------------
;; info
;;------------------------------
(defn m-info
  "The pattern `*@meta@name` is used to find all
   mp-names available at short term memory."  
  []
  (run! prn
        (map st/key->mp-name
             (st/pat->keys "*@meta@name"))))

(defn l-info
  "Returns a list with the currently registered listener
  patterns."
  []
  (run! prn
        (keys (deref st/listeners))))

;;------------------------------
;; status (stat)
;;------------------------------
(defn c-status
  "Returns the  **c**ontainer status.
  Returns the state map for the `i` container."
  ([i]
   (c-status (->mp-id) i))
  ([mp-id i]
   (u/print-vec-map (state/cont-status mp-id i))))

(defn n-status
  "Returns  defi**n**itions status.
  Returns the `state map` for the `i`
  definitions structure."
  ([i]
   (n-status (->mp-id) i))
  ([mp-id i]
   (u/print-vec-map (state/defins-status mp-id i))))

;;------------------------------
;; build mpd
;;------------------------------
(defn m-build
  "Loads a mpd from long term memory and
  builds the short term memory. The `mp-id`
  may be set with [[workon!]].  
  
  Usage:  
  ```clojure
  (m-build mpid)
  ;; or
  (workon! mpid)
  ;; followed by
  (m-build)
  ```"
  ([]
   (m-build (->mp-id)))
  ([mp-id]
   (timbre/info "build " mp-id)
   (->> mp-id
        (u/compl-main-path)
        (lt/id->doc)
        (u/doc->safe-doc)
        (bld/store))))

(defn m-build-edn
  "Builds up a the mpds in `edn` format provided by *cmp*
  (see resources directory).
  
  ```clojure
  (m-build-edn \"resources/mpd-modbus.edn\")
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
(defn d-add
  "Adds a doc to the api to store the resuls in."
  ([doc-id]
   (d-add (->mp-id) doc-id))
  ([mp-id doc-id]
   (doc/add mp-id doc-id)))

(defn d-rm
  "Removes a doc from the api."
  ([doc-id]
   (d-rm (->mp-id) doc-id))
  ([mp-id doc-id]
   (doc/rm mp-id doc-id)))

(defn d-ids
  "Gets a list of ids added."
  ([]
   (d-ids (->mp-id)))
  ([mp-id]
   (doc/ids mp-id)))


;;------------------------------
;; check mp tasks
;;------------------------------
(defn check
  "
  REVIEW
  
  Check the tasks of the `container` and
  `definitions` structure. *cmp* does not preload
  the tasks, they are loaded from the `lt-mem`
  during runtime."
  ([]
   (check (->mp-id)))
  ([mp-id]
   (let [p         (u/main-path mp-id)
         k-ncont   (st/meta-ncont-path p)
         n-cont    (st/key->val k-ncont)
         k-ndefins (st/meta-ndefins-path p)
         n-defins  (st/key->val k-ndefins)]
     (run!
      (fn [i]
        (chk/struct-tasks (st/cont-defin-path p i)))
      (range n-cont))
     (run!
      (fn [i]
        (chk/struct-tasks (st/defins-defin-path p i)))
      (range n-defins)))))

;;------------------------------
;; start observing mp
;;------------------------------
(defn m-start
  "Registers a listener for the `ctrl`
  interface of a `mp-id` (see [[workon!]])."
  ([]
   (m-start (->mp-id)))
  ([mp-id]
   (ctrl/start mp-id)))

;;------------------------------
;; stop observing 
;;------------------------------
(defn m-stop
  "De-registers the listener for the `ctrl`
  interface of the given `mp-id` (see [[workon!]])."
  ([]
   (m-stop (->mp-id)))
  ([mp-id]
   (ctrl/stop mp-id)))

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
   (st/set-val! (st/cont-ctrl-path  mp-id i) cmd)))

(defn c-run
  "Shortcut to push a `run` to the control
  interface of  mp container `i`."
  [i]
  (set-ctrl (->mp-id) i "run"))


(defn c-stop
  "Shortcut to push a `stop` to the control
  interface of  mp container `i`."
  [i]
  (set-ctrl (->mp-id) i "stop"))

(defn c-reset
  "Shortcut to push a `reset` to the control
  interface of  mp container `i`. The `reset` cmd
  **don't**  de-register the `state` listener so
  that the container starts from the beginning.
  **reset is a container restart**
  "
  [i]
  (set-ctrl (->mp-id) i "reset"))

(defn c-suspend
  "Shortcut to push a `suspend` to the control
  interface of  mp container `i`. The `suspend` cmd
  de-register the `state` listener and leaves the state
  as it is.
  "
  [i]
  (set-ctrl (->mp-id) i "suspend"))

;;------------------------------
;; tasks
;;------------------------------
(defn t-build
  "Builds the `tasks` endpoint. At
  runtime all `tasks` are provided by
  `st-mem`. The advantage is: tasks
  can be modified at runtime." 
  []
  (bld/store-tasks (lt/all-tasks)))

(defn t-list-action
  "Prints a list of all tasks stored in
  **short term memory**. If a `action` is given
  it is used as a filter."
  ([]
   (t-list-action :all))
  ([action]
  (run!
   (fn [k]
     (let [task   (st/key->val k)
           name   (:TaskName task)
           acc    (:Action task)]
       (if (= :all action)
         (u/print-kvv k name acc))
       (if (= acc action)
         (u/print-kvv k name action))))   
  (st/key->keys "tasks"))))

(defn t-build-edn
  "Stores the `task` slurping from the files
  given in `resources/config.edn`

  Usage:
  
  ```clojure
  (t-build-edn)
  ```"
  []
  (run!
   (fn [uri]
     (timbre/info "try to slurp and build: " uri  )
       (bld/store-task
        (read-string
         (slurp uri))))
     (cfg/edn-tasks (cfg/config))))


(defn t-clear
  "Function removes all keys starting with `tasks`."  
  []
  (st/clear "tasks"))

(defn t-refresh
  "Refreshs the `tasks` endpoint.
  
  Usage:
  
  ```clojure
  (t-refresh)
  ```
  "
  []
  (timbre/info "clear tasks")
  (t-clear)
  (timbre/info "build tasks from db")
  (t-build)
  (timbre/info "build edn tasks")
  (t-build-edn))

;;------------------------------
;; clear mpd
;;------------------------------
(defn m-clear
  "Clears all short term memory for the given `mp-id`
  (see [[workon!]]).
   Usage:
  
  ```clojure
  (m-clear mpid)
  ;; or
  (workon! mpid)
  (m-clear)
  
  ```"
  ([]
   (m-clear (->mp-id)))
  ([mp-id]
   (m-stop mp-id)
   (st/clear mp-id)))
