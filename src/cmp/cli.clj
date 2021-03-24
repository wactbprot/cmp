(ns cmp.cli
  ^{:author "wactbprot"
    :doc "Frequently needed cli funcions."}
  
  (:require [cmp.st-build            :as build]
            [cmp.config              :as config]
            [cmp.doc                 :as doc]
            [cmp.lt-mem              :as lt]
            [clojure.pprint          :as pp]
            [cmp.st-mem              :as st]
            [cmp.st-utils            :as stu]
            [com.brunobonacci.mulog  :as mu]
            [cmp.state               :as state]
            [cmp.utils               :as utils]
            [cmp.work                :as work])
  (:use    [clojure.repl]))

(comment
  (def conf (config/config)))

;;------------------------------
;; logging system
;;------------------------------
(defonce logger (atom nil))

(defn init-log!
  [{conf :mulog }]
  (mu/set-global-context! {:app-name "cmp"})
  (mu/start-publisher! conf))

(defn stop-log!
  [conf]
  (mu/log ::stop)
  (@logger)
  (reset! logger nil))

(defn start-log!
  [conf]
  (mu/log ::start)
  (reset! logger (init-log! conf)))

;;------------------------------
;; start observing mp
;;------------------------------
(defn m-start
  "Registers a listener for the `ctrl` interface of a
  `mp-id`."
  [conf mp-id]
  (state/start mp-id))

;;------------------------------
;; stop observing
;;------------------------------
(defn m-stop
  "De-registers the listener for the `ctrl` interface of the given
  `mp-id` (see [[workon!]])."
  [conf mp-id]
  (state/stop mp-id))

;;------------------------------
;; build mpd from lt mem
;;------------------------------
(defn m-build-ltm
  "Loads a mpd from long term memory and builds up a `st-mem` version of
  it. The `mp-id` may be set with [[workon!]]. [[m-start]] is called
  after mp is build.

  Example:
  ```clojure
  (m-build {} mpid)
  ```"
  ([conf mp-id]
   (m-stop conf mp-id)
   (st/clear! mp-id)
   (->> mp-id utils/compl-main-path lt/get-doc utils/doc->safe-doc build/store)
   (m-start conf mp-id)))

;;------------------------------
;; build ref mpd
;;------------------------------
(defn m-build-ref
  "Builds up the `ref`erence mpd provided in `edn` format in the
  resources folder."
  [conf]
  (let [doc   (config/ref-mpd conf) 
        mp-id (utils/extr-main-path (:_id doc))]
    (m-stop conf mp-id)
    (st/clear! mp-id)
    (build/store doc)
    (m-start conf mp-id)))

;;------------------------------
;; build tasks
;;------------------------------
(defn t-build
  "Builds the `tasks` endpoint. At runtime all `tasks` are provided by
  `st-mem`. The advantage is: tasks can be modified at runtime."
  [conf]
  (build/store-tasks (lt/all-tasks)))

(defn t-clear
  "Function removes all keys starting with `tasks`."
  [conf]
  (st/clear! (stu/task-prefix)))

(defn t-refresh
  "Refreshs the `tasks` endpoint.

  Example:
  ```clojure
  (t-refresh {})
  ```"
  [conf]
  (t-clear conf)
  (t-build conf))

(defn t-run-by-key
  "Runs the task the key is related to.

  Example:
  ```clojure
  (t-run-by-key {} \"ce3-cmp_calib@container@000@state@000@000\")
  ```"
  [conf k]
  (work/check k))  
;;------------------------------
;; documents
;;------------------------------
(defn d-add
  "Adds a doc to the api to store the resuls in."
  [conf mp-id doc-id]
  (doc/add mp-id doc-id))

(defn d-rm
  "Removes a doc from the api."
  [conf mp-id doc-id]
  (doc/rm mp-id doc-id))

(defn d-ids
  "Gets a list of ids added."
  [conf mp-id]
  (doc/ids mp-id))

;;------------------------------
;; push ctrl commands
;;------------------------------
(defn set-ctrl
  "Writes the command string (`cmd`) to the control interface of a
  `mpd`. If the `mpd` is already started (see [[m-start]]) the next
  steps work as follows: `cmd` is written to the short term memory by
  means of [[cmp.st-mem.set-val!]].  The writing process triggers the
  `registered` `callback` (registered by [[m-start]]). The `callback`
  cares about the `cmd`.  `cmd`s are:

  * `\"run\"`
  * `\"stop\"`
  * `\"mon\"`
  * `\"suspend\"`

  ```clojure
  (set-ctrl {} \"ref\" 0\"run\")
  ```

  **NOTE:**

  `set-ctrl` only writes to the `container` structure.  The
  `definitions` struct should not be started by a
  user."

  [conf mp-id i cmd]
  (st/set-val! (stu/cont-ctrl-key mp-id (utils/lp i)) cmd))

(defn c-run
  "Shortcut to push a `run` to the control interface of mp container
  `i`."
  [conf mp-id i]
  (set-ctrl conf mp-id i "run"))

(defn c-mon
  "Shortcut to push a `mon` to the control interface of mp container
  `i`."
  [conf mp-id i]
  (set-ctrl conf mp-id i "mon"))

(defn c-stop
  "Shortcut to push a `stop` to the control interface of mp container
  `i`."
  [conf mp-id i]
  (set-ctrl conf mp-id i "stop"))

(defn c-reset
  "Shortcut to push a `reset` to the control interface of mp container
  `i`. The `reset` cmd **don't** de-register the `state` listener so
  that the container starts from the beginning.  **reset is a
  container restart**"
  [conf mp-id i]
  (set-ctrl conf mp-id i "reset"))

(defn c-suspend
  "Shortcut to push a `suspend` to the control interface of mp container
  `i`. The `suspend` cmd de-register the `state` listener and leaves
  the state as it is."
  [conf mp-id i]
  (set-ctrl conf mp-id i "suspend"))
  
