(ns cmp.core
  ^{:author "wactbprot"
    :doc "Provides the api of cmp. `(m-start)`, `(m-stop)` etc.  are
          intended for **repl** use only. Graphical user interfaces
          should attache to the **short term memory**."}
   (:require [cmp.build                :as build]
             [cmp.config               :as cfg]
             [cmp.doc                  :as d]
             [cmp.key-utils            :as ku]
             [cmp.lt-mem               :as lt]
             [com.brunobonacci.mulog   :as mu]
             [portal.api               :as p]
             [cmp.st-mem               :as st]
             [cmp.state                :as state]
             [cmp.task                 :as task]
             [cmp.utils                :as u]
             [cmp.work                 :as w]
             [portal.api :as p]))

;;------------------------------
;; log system
;;------------------------------
(def logger (atom nil))

(defn init-log!
  [{conf :mulog }]
  (mu/set-global-context!
   {:app-name "cmp" })
  (mu/start-publisher! conf))

(defn stop-log!
  []
  (mu/log ::stop)
  (@logger)
  (reset! logger nil))

(defn start-log!
  []
  (mu/log ::start)
  (reset! logger (init-log! (cfg/config))))

;;------------------------------
;; current-mp atom and workon
;;------------------------------
(def current-mp
  "Provides a storing place for the current mp-id for convenience. Due
  to this atom the `(build)`, `(check)` or `(start)` function needs no
  argument."
  (atom "ref"))

(defn workon!
  "Sets the mpd to work on (see [[current-mp]]).

  Example:
  ```clojure
  (workon! 'se3-calib')
  (deref current-mp)
  ```"
  [mp-id]
  (reset! current-mp mp-id))

;;------------------------------
;; info
;;------------------------------
(defn m-data
  "Returns a info map about the mpd with the id `mp-id`."
  ([]
   (m-data (deref current-mp)))
  ([mp-id]
   {:mp-id      mp-id
    :mp-descr   (u/short-string (st/key->val (ku/meta-descr-key mp-id)))
    :mp-std     (st/key->val (ku/meta-std-key mp-id))
    :mp-ncont   (st/key->val (ku/meta-ncont-key mp-id))
    :mp-ndefins (st/key->val (ku/meta-ndefins-key mp-id))}))

(defn ms-data
  "The pattern `*@meta@name` is used to find all `mp-id`s loaded and
  available at the short term memory."
  []
  (mapv (fn [k] (m-data (ku/key->mp-id k)))
        (st/pat->keys "*@meta@name")))

(defn c-data
  "Returns a map about the `i`th container of the mpd with the id
  `mp-id`."
  ([]
   (c-data (deref current-mp) 0))
  ([i]
   (c-data (deref current-mp) i))
  ([mp-id i]
   (let [idx (u/lp i)]
     {:c-no-idx idx 
      :c-title  (st/key->val (ku/cont-title-key mp-id idx))
      :c-descr  (st/key->val (ku/cont-descr-key mp-id idx))
      :c-ctrl   (st/key->val (ku/cont-ctrl-key mp-id idx))
      :c-status (state/cont-status mp-id idx)})))
  
(defn n-data
  "Returns a map about the `i`th defi**n**itions of the mpd with the id
  `mp-id`."
  ([]
   (c-data (deref current-mp) 0))
  ([i]
   (c-data (deref current-mp) i))
  ([mp-id i]
   (let [idx (u/lp i)]
     {:n-no-idx  idx
      :n-title  (st/key->val (ku/defins-class-key mp-id idx))
      :n-descr  (st/key->val (ku/defins-descr-key mp-id idx))
      :n-ctrl   (st/key->val (ku/defins-ctrl-key mp-id idx))
      :n-status (state/defins-status mp-id idx)})))

(defn cs-data
  "Returns info about the containers of the mpd with the id `mp-id`."
  ([]
   (cs-data (deref current-mp)))
  ([mp-id]
   (mapv (fn [k] (c-data mp-id (ku/key->no-idx k)))
         (sort (st/pat->keys (ku/cont-title-key mp-id "*"))))))
;;------------------------------
;; listeners
;;------------------------------
(defn l-data
  "Returns a table with the currently registered listener patterns."
  []
  (mapv (fn [[k v]] {:key k :val v}) (deref st/listeners)))

;;------------------------------
;; worker futures
;;------------------------------
(defn w-data
  "Returns data about the currently registered worker futures."
  ([]
   (w-data false))
  ([deref?]
   (mapv (fn [[k v]]
           {:key k :val (if (if deref? (deref v) v) "ok" v)})
         (deref w/future-reg))))

;;------------------------------
;; start observing mp
;;------------------------------
(defn m-start
  "Registers a listener for the `ctrl` interface of a
  `mp-id` (see [[workon!]])."
  ([]
   (m-start (deref current-mp)))
  ([mp-id]
   (state/start mp-id)))

;;------------------------------
;; stop observing
;;------------------------------
(defn m-stop
  "De-registers the listener for the `ctrl` interface of the given
  `mp-id` (see [[workon!]])."
  ([]
   (m-stop (deref current-mp)))
  ([mp-id]
   (state/stop mp-id)))

;;------------------------------
;; build mpd
;;------------------------------
(defn m-build
  "Loads a mpd from long term memory and builds the short term
  memory. The `mp-id` may be set with [[workon!]]. [[m-start]] is
  called after mp is build.

  Usage:
  ```clojure
  (m-build mpid)
  ;; or
  (workon! mpid)
  ;; followed by
  (m-build)
  ```"
  ([]
   (m-build (deref current-mp)))
  ([mp-id]
   (println "build " mp-id)
   (->> mp-id u/compl-main-path lt/id->doc u/doc->safe-doc build/store)
   (m-start mp-id)))

(defn m-build-edn
  "Builds up a the mpds in `edn` format provided by *cmp* (see resources
  directory).

  ```clojure
  (m-build-edn \"resources/mpd-devhub.edn\")
  ```"
  []
  (run! (fn [uri]
          (println "try to slurp and build: " uri)
          (-> uri slurp read-string build/store))
        (cfg/edn-mpds (cfg/config))))

;;------------------------------
;; documents
;;------------------------------
(defn d-add
  "Adds a doc to the api to store the resuls in."
  ([doc-id]
   (d-add (deref current-mp) doc-id))
  ([mp-id doc-id]
   (d/add mp-id doc-id)))

(defn d-rm
  "Removes a doc from the api."
  ([doc-id]
   (d-rm (deref current-mp) doc-id))
  ([mp-id doc-id]
   (d/rm mp-id doc-id)))

(defn d-ids
  "Gets a list of ids added."
  ([]
   (d-ids (deref current-mp)))
  ([mp-id]
   (d/ids mp-id)))

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

  The `mp-id` parameter may be give directly

  ```clojure
  (set-ctrl \"ref\" \"run\")
  ```
  or is derived from `(deref current-mp)` [[workon \"ref\"]].


  ```clojure
  (workon! \"ref\"=
  (set-ctrl \"run\")
  ```

  **NOTE:**

  `set-ctrl` only writes to the `container` structure.  The
  `definitions` struct should not be started by a
  user (see [[workon!]])."
  ([i cmd]
   (set-ctrl (deref current-mp) i cmd))
  ([mp-id i cmd]
   (st/set-val! (ku/cont-ctrl-key mp-id (u/lp i)) cmd)))

(defn c-run
  "Shortcut to push a `run` to the control interface of mp container
  `i`."
  [i]
  (set-ctrl (deref current-mp) i "run"))

(defn c-mon
  "Shortcut to push a `mon` to the control interface of mp container
  `i`."
  [i]
  (set-ctrl (deref current-mp) i "mon"))

(defn c-stop
  "Shortcut to push a `stop` to the control interface of mp container
  `i`."
  [i]
  (set-ctrl (deref current-mp) i "stop"))

(defn c-reset
  "Shortcut to push a `reset` to the control interface of mp container
  `i`. The `reset` cmd **don't** de-register the `state` listener so
  that the container starts from the beginning.  **reset is a
  container restart**"
  [i]
  (set-ctrl (deref current-mp) i "reset"))

(defn c-suspend
  "Shortcut to push a `suspend` to the control interface of mp container
  `i`. The `suspend` cmd de-register the `state` listener and leaves
  the state as it is."
  [i]
  (set-ctrl (deref current-mp) i "suspend"))

;;------------------------------
;; tasks
;;------------------------------
(defn t-build
  "Builds the `tasks` endpoint. At runtime all `tasks` are provided by
  `st-mem`. The advantage is: tasks can be modified at runtime."
  []
  (build/store-tasks (lt/all-tasks)))

(defn ts-data
  "Returns a vector of **assembled tasks** stored in **short term
  memory**. If a `kw` and `val` are given it is used as a filter

  Example:
  ```clojure
  (t-data)
  ;; same as
  (t-data :Action :all)
  ;;
  (t-data :Action \"TCP\")
  ;;
  (t-data :Port \"23\" \"ref\")
  ```"
  ([]
   (ts-data  :Action :all "core" "test" 0 0 0))
  ([kw v]
   (ts-data  kw v "core" "test" 0 0 0))
  ([kw v mp-id]
   (ts-data  kw v mp-id "test" 0 0 0))
  ([kw v mp-id struct i j k]
   (let [state-key (ku/vec->key [mp-id struct (u/lp i) "state" (u/lp j) (u/lp k)])]
     (filter some? (mapv (fn [k]
                           (let [m (task/assemble (task/gen-meta-task (ku/key->struct k)) mp-id state-key)
                                 x (kw m)]
                             (when (and x (or (= x v) (= :all v)))
                               (assoc m :stm-key k))))
                         (st/key->keys (ku/task-prefix)))))))

(defn t-run
  "Runs the task with the given name (from stm).
  If only the name is provided, results are stored under
  `core@test@0@response@0@0`.

  If `mp-id`, `struct`, `i`, `j` and `k` is given, the results are
  written to `<mp-id@<struct>@<i>@response@<j>@<k>`.  A listener at
  this key triggers a `cb!` which de-registers and closes the
  listener. The callback also gets the value of the
  key (`<mp-id@<struct>@<i>@response@<j>@<k>`) and pretty prints it.

  REVIEW function is way to large

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
  
  Debug:
  ```clojure
  @st/listeners
  (st/de-register! \"core\" \"test\" 0 \"response\")
  ```"
  ([t]
   (t-run t "core" "test" 0 0 0))
  ([t mp-id]
   (t-run t mp-id "test" 0 0 0))
  ([t mp-id struct no-idx seq-idx par-idx]
   (let [no-idx    (u/lp no-idx)
         seq-idx   (u/lp seq-idx)
         par-idx   (u/lp par-idx)
         func       "response"
         state-key  (ku/vec->key [mp-id struct no-idx "state" seq-idx par-idx])
         resp-key   (ku/vec->key [mp-id struct no-idx func    seq-idx par-idx])
         meta-task  (task/gen-meta-task t)
         task       (task/assemble meta-task mp-id state-key)]
     (when (task/dev-action? task)
       (println "task dispached, wait for response...")
       (st/register! mp-id struct no-idx func (fn [msg]
                                                (when-let [result-key (st/msg->key msg)]
                                                  (st/de-register! mp-id struct no-idx func)
                                                  (st/key->val result-key)))))
     (w/check task))))

(defn t-run-by-key
  "Calls `t-run` after extracting key info.  A call with all all kinds
  of complete keys `k` is ok.  Complete means: the functions:

  *  `(ku/key->mp-id   k)`
  *  `(ku/key->struct  k)`
  *  `(ku/key->no-idx  k)`
  *  `(ku/key->seq-idx k)`
  *  `(ku/key->par-idx k)`

  don't return `nil`.

  Example:
  ```clojure
  (t-run-by-key \"se3-cmp_state@container@006@state@000@000\")
  ```"
  [k]
  (let [mp-id     (ku/key->mp-id   k)
        struct    (ku/key->struct  k)
        no-idx    (ku/key->no-idx  k)
        seq-idx   (ku/key->seq-idx k)
        par-idx   (ku/key->par-idx k)
        def-key   (ku/vec->key [mp-id struct no-idx "definition" seq-idx par-idx])
        t         (st/key->val def-key)]
    (if t
      (t-run t mp-id struct no-idx seq-idx par-idx)
      (println (str "no TaskName at key: " k)))))

(defn t-raw
  "Shows the raw task as stored at st-memory"
  [s]
  (st/key->val (ku/task-key s)))

(defn t-assemble
  "Assembles the task with the given `x` (`TaskName` or `{:TaskName
  \"task-name\" :Replace {:%waittime 3000}}`).  If a `mp-id` (default
  is `\"core\"`) is given `FromExchange` dependencies may be resolved.

  Example:
  ```clojure
  (t-assemble {:TaskName \"Common-wait\"
           :Replace {\"%waittime\" 3000}})
  ```"
  ([x]
   (t-assemble x "core" "test" 0 0 0))
  ([x mp-id]
   (t-assemble x mp-id "test" 0 0 0))
  ([x mp-id struct i j k]
   (let [state-key  (ku/vec->key [mp-id struct (u/lp i) "state" (u/lp j) (u/lp k)])
         meta-task  (task/gen-meta-task x)]
     (task/assemble meta-task mp-id state-key))))
  
(defn t-clear
  "Function removes all keys starting with `tasks`."
  []
  (st/clear! (ku/task-prefix)))

(defn t-refresh
  "Refreshs the `tasks` endpoint.

  Example:
  ```clojure
  (t-refresh)
  ```"
  []
  (println "clear tasks")
  (t-clear)
  (println "build tasks from db")
  (t-build))

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
   (m-clear (deref current-mp)))
  ([mp-id]
   (m-stop mp-id)
   (println "mp stoped")
   (st/clean-register! mp-id)
   (println "mp de registered")
   (st/clear! mp-id)
   (println "mp cleared")))

;;------------------------------
;; Exchange table
;;------------------------------
(defn e-data
  "Returns a vector of the `exchange`-interface data of the
  mpd with the id `mp-id`.
  ```"
  ([]
   (e-data  (deref current-mp)))
  ([mp-id]
   (mapv (fn [k]
           {:key k :value (st/key->val k)})
         (st/key->keys (ku/exch-prefix mp-id)))))
