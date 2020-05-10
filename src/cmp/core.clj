(ns cmp.core
  ^{:author "wactbprot"
    :doc "Provides the api of cmp. `(start)`, `(stop)` etc. 
          are intended for **repl** use only. Graphical user 
          interfaces should attache to the **short term memory**."}
  (:require [cmp.lt-mem :as lt]
            [cmp.st-mem :as st]
            [clojure.core.async :as a]
            [clojure.pprint :as pp]
            [cmp.utils :as u]
            [cmp.doc :as doc]
            [cmp.config :as cfg]
            [cmp.build :as build]
            [cmp.task :as task]
            [cmp.check :as chk]
            [cmp.ctrl :as ctrl]
            [cmp.state :as state]
            [cmp.work :as work]
            [cmp.pubsubstat :as p]
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
  @current-mp-id
  ```
  "
  [mp-id]
  (reset! current-mp-id mp-id))

;;------------------------------
;; info
;;------------------------------
(defn m-info
  "The pattern `*@meta@name` is used to find all
   mp-names available at short term memory."  
  []
  (run! prn
        (map st/key->mp-id
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
   (c-status @current-mp-id i))
  ([mp-id i]
   (pp/print-table (state/cont-status mp-id i))))

(defn n-status
  "Returns  defi**n**itions status.
  Returns the `state map` for the `i`
  definitions structure."
  ([i]
   (n-status @current-mp-id i))
  ([mp-id i]
   (pp/print-table (state/defins-status mp-id i))))

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
   (m-build @current-mp-id))
  ([mp-id]
   (timbre/info "build " mp-id)
   (->> mp-id
        (u/compl-main-path)
        (lt/id->doc)
        (u/doc->safe-doc)
        (build/store))))

(defn m-build-edn
  "Builds up a the mpds in `edn` format provided by *cmp*
  (see resources directory).
  
  ```clojure
  (m-build-edn \"resources/mpd-devhub.edn\")
  ```
  "
  []
  (run!
   (fn [uri]
     (timbre/info "try to slurp and build: " uri  )
     (build/store
      (read-string
       (slurp uri))))
   (cfg/edn-mpds (cfg/config))))

;;------------------------------
;; documents
;;------------------------------
(defn d-add
  "Adds a doc to the api to store the resuls in."
  ([doc-id]
   (d-add @current-mp-id doc-id))
  ([mp-id doc-id]
   (doc/add mp-id doc-id)))

(defn d-rm
  "Removes a doc from the api."
  ([doc-id]
   (d-rm @current-mp-id doc-id))
  ([mp-id doc-id]
   (doc/rm mp-id doc-id)))

(defn d-ids
  "Gets a list of ids added."
  ([]
   (d-ids @current-mp-id))
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
   (check @current-mp-id))
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
   (m-start @current-mp-id))
  ([mp-id]
   (ctrl/start mp-id)))

;;------------------------------
;; stop observing 
;;------------------------------
(defn m-stop
  "De-registers the listener for the `ctrl`
  interface of the given `mp-id` (see [[workon!]])."
  ([]
   (m-stop @current-mp-id))
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

  The `mp-id` is received over `@current-mp-id`.
  **NOTE:** The `definitions` struct should not
  be started by user (see [[workon!]])."
  ([i cmd]
   (set-ctrl @current-mp-id i cmd))
  ([mp-id i cmd]
   (st/set-val! (st/cont-ctrl-path  mp-id i) cmd)))

(defn c-run
  "Shortcut to push a `run` to the control
  interface of  mp container `i`."
  [i]
  (set-ctrl @current-mp-id i "run"))


(defn c-stop
  "Shortcut to push a `stop` to the control
  interface of  mp container `i`."
  [i]
  (set-ctrl @current-mp-id i "stop"))

(defn c-reset
  "Shortcut to push a `reset` to the control
  interface of  mp container `i`. The `reset` cmd
  **don't**  de-register the `state` listener so
  that the container starts from the beginning.
  **reset is a container restart**
  "
  [i]
  (set-ctrl @current-mp-id i "reset"))

(defn c-suspend
  "Shortcut to push a `suspend` to the control
  interface of  mp container `i`. The `suspend` cmd
  de-register the `state` listener and leaves the state
  as it is.
  "
  [i]
  (set-ctrl @current-mp-id i "suspend"))

;;------------------------------
;; tasks
;;------------------------------
(defn t-build
  "Builds the `tasks` endpoint. At
  runtime all `tasks` are provided by
  `st-mem`. The advantage is: tasks
  can be modified at runtime." 
  []
  (build/store-tasks (lt/all-tasks)))

(defn t-table
  "Prints a table of **assembled tasks** stored in
  **short term memory**. If a `kw` and `val` are given
  it is used as a filter

  Example:
  ```clojure
  (t-table)
  ;; same as
  (t-table :Action :all)
  ;;
  (t-table :Action \"TCP\")
  ;;
  (t-table :Port \"23\" \"ref\")
  ```
  . "
  ([]
   (t-table  :Action :all "core" "test" 0 0 0))
  ([kw v]
   (t-table  kw v "core" "test" 0 0 0))
  ([kw v mp-id]
   (t-table  kw v mp-id "test" 0 0 0))
  ([kw v mp-id struct i j k]
   (let [state-key (u/vec->key [mp-id struct i "state" j k])]
     (pp/print-table
      (filter some?
              (into []
                    (map (fn [k]
                           (let [name      (st/key->struct k)
                                 meta-task (task/gen-meta-task name)
                                 task      (task/assemble meta-task mp-id state-key)
                                 value     (kw task)]
                             (if (and value (or (= value v) (= :all v)))
                               {kw value :TaskName name :stm-key k} )))
                         (st/key->keys "tasks"))))))))

(defn t-run
  "Runs the task with the given name (from stm).
  If only the name is provided, results are stored
  under  `core@test@0@response@0@0`.

  If  `mp-id`, `struct`, `i`, `j` and  `k` is given,
  the results are written to `<mp-id@<struct>@<i>@response@<j>@<k>`.
  A listener at this key triggers a `cb!` which de-registers
  and closes the listener. The callback also gets the value of 
  the key (`<mp-id@<struct>@<i>@response@<j>@<k>`) and pretty
  prints it.
  
  Example:
  ```clojure
  (t-run \"DKM_PPC4_DMM-read_temp\")
  ;;
  ;; {:t_start 1588071759882,
  ;; :t_stop 1588071768996,
  ;; :Result
  ;; [{:Type dkmppc4,
  ;;  :Value 24.297828639,
  ;;  :Unit C,
  ;;  :SdValue 0.0013625169107,
  ;;  :N 10}]}
  ```

  Debug
  ```clojure
  @st/listeners
  (st/de-register! \"core\" \"test\" 0 \"response\")
  ```
  "
  ([name]
   (t-run name "core" "test" 0 0 0))
  ([name mp-id struct i j k]
   (let [func       "response"
         state-key  (u/vec->key[mp-id struct i "state" j k])
         resp-key   (u/vec->key[mp-id struct i func j k])
         meta-task  (task/gen-meta-task name)
         task       (task/assemble meta-task mp-id state-key)]
     (when (task/dev-action? task)
       (timbre/info "task dispached, wait for response...")
       (st/register! mp-id struct i func (fn [msg]
                                           (when-let [k (st/msg->key msg)]
                                             (st/de-register! mp-id struct i func)
                                             (pp/pprint (st/key->val k))))))
     (work/dispatch! task state-key))))

(defn t-raw
  "Shows the raw task as stored at st-memory" 
  [s]
  (st/key->val (u/vec->key ["tasks" s])))

(defn t-assemble
  "Assembles the task with the given `x`
  (`TaskName` or
  `{:TaskName \"task-name\" :Replace {:%waittime 3000}}`).
  If a `mp-id` (default is `\"core\"`) is given
  `FromExchange` dependencies may be resolved.

  Example:
  ```clojure
  (t-assemble {:TaskName \"Common-wait\"
           :Replace {\"%waittime\" 3000}})
  ```
  "
  ([x]
   (t-assemble x "core" "test" 0 0 0))
  ([x mp-id]
   (t-assemble x mp-id "test" 0 0 0))
  ([x mp-id struct i j k]
   (let [state-key  (u/vec->key[mp-id struct i "state" j k])
         meta-task  (task/gen-meta-task x)]
     (task/assemble meta-task mp-id state-key))))
  
(defn t-build-edn
  "Stores the `task` slurped from the files
  configured in `resources/config.edn`.

  Example:
  ```clojure
  (t-build-edn)
  ```"
  []
  (run!
   (fn [uri]
     (timbre/info "try to slurp and build: " uri  )
       (build/store-task
        (read-string
         (slurp uri))))
     (cfg/edn-tasks (cfg/config))))


(defn t-clear
  "Function removes all keys starting with `tasks`."  
  []
  (st/clear "tasks"))

(defn t-refresh
  "Refreshs the `tasks` endpoint.
  
  Example:
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

  Example:
  ```clojure
  (m-clear mpid)
  ;; or
  (workon! mpid)
  (m-clear)
  
  ```"
  ([]
   (m-clear @current-mp-id))
  ([mp-id]
   (m-stop mp-id)
   (st/clear mp-id)))
