 (ns cmp.core
  ^{:author "wactbprot"
    :doc "Provides the api of cmp. `(m-start)`, `(m-stop)` etc.  are
          intended for **repl** use only. Graphical user interfaces
          should attache to the **short term memory**."}
   (:require [cmp.build       :as build]
             [cmp.config      :as cfg]
             [cmp.doc         :as d]
             [cmp.key-utils   :as ku]
             [cmp.log         :as log]
             [cmp.lt-mem      :as lt]
             [clojure.pprint  :as pp]
             [cmp.st-mem      :as st]
             [cmp.state       :as state]
             [cmp.task        :as task]
             [taoensso.timbre :as timbre]
             [cmp.utils       :as u]
             [cmp.work        :as work]))

;;------------------------------
;; current-mp-id atom and workon
;;------------------------------
(def current-mp-id
  "Provides a storing place for the current mp-id for convenience. Due
  to this atom the `(build)`, `(check)` or `(start)` function needs no
  argument." 
  (atom "ref"))

(defn workon!
  "Sets the mpd to work on (see [[current-mp-id]]).
  
  Usage:
    ```clojure
  (workon! 'se3-calib')
  (deref current-mp-id)
  ```
  "
  [mp-id]
  (reset! current-mp-id mp-id))

;;------------------------------
;; info
;;------------------------------
(defn m-info
  "Returns a info map about the mpd with the id `mp-id`."
  ([]
   (m-info (deref current-mp-id)))
  ([mp-id]
   {:mp-id      mp-id
    :mp-descr   (u/short-string (st/key->val (ku/meta-descr-key mp-id)))
    :mp-std     (st/key->val (ku/meta-std-key mp-id))
    :mp-ncont   (st/key->val (ku/meta-ncont-key mp-id))
    :mp-ndefins (st/key->val (ku/meta-ndefins-key mp-id))}))

(defn ms-info
  "The pattern `*@meta@name` is used to find all `mp-id`s loaded and
  available at the short term memory."
  []
  (pp/print-table
   (mapv (fn [k] (m-info (ku/key->mp-id k)))
         (st/pat->keys "*@meta@name"))))

(defn c-data
  "Returns a map about the `i`th container of the mpd with the id
  `mp-id`."
  [mp-id i]
  {:c-no-idx (u/lp i) 
   :c-title  (st/key->val    (ku/cont-title-key mp-id (u/lp i)))
   :c-descr  (u/short-string (st/key->val (ku/cont-descr-key mp-id (u/lp i))))
   :c-ctrl   (st/key->val    (ku/cont-ctrl-key mp-id (u/lp i)))})

(defn c-info
  "Returns info about the container `i` of the mpd with the id
  `mp-id`."
  ([]
   (c-info (deref current-mp-id) 0))
  ([i]
   (c-info (deref current-mp-id) i))
  ([mp-id i]
   (pp/print-table [(c-data mp-id i)])))
   
(defn cs-info
  "Returns info about the containers of the mpd with the id `mp-id`."
  ([]
   (cs-info (deref current-mp-id)))
  ([mp-id]
   (pp/print-table
    (mapv
     (fn [k] (c-data mp-id (ku/key->no-idx k)))
     (sort (st/pat->keys (ku/cont-title-key mp-id "*")))))))

  
;;------------------------------
;; listeners 
;;------------------------------
(defn l-info
  "Returns a table with the currently registered listener patterns."
  []
  (pp/print-table
   (mapv
    (fn [[k v]]
      {:key k :val v})
    (deref st/listeners))))

;;------------------------------
;; worker futures 
;;------------------------------
(defn w-info
  "Returns a table with the currently registered worker futures."
  ([]
   (w-info false))
  ([deref?]
   (pp/print-table
    (mapv
     (fn [[k v]]
       {:key k :val (if (nil? (if deref? (deref v) v)) "ok" v)})
     (deref work/future-reg)))))
  
;;------------------------------
;; status (stat)
;;------------------------------
(defn c-status
  "Returns the **c**ontainer status.  Returns the state map for the `i`
  container."
  ([i]
   (c-status (deref current-mp-id) i))
  ([mp-id i]
   (pp/print-table (state/cont-status mp-id (u/lp i)))))

(defn n-status
  "Returns defi**n**itions status. Returns the `state map` for the `i`
  definitions structure."
  ([i]
   (n-status (deref current-mp-id) i))
  ([mp-id i]
   (pp/print-table (state/defins-status mp-id (u/lp i)))))

;;------------------------------
;; build mpd
;;------------------------------
(defn m-build
  "Loads a mpd from long term memory and builds the short term
  memory. The `mp-id` may be set with [[workon!]].
  
  Usage:  
  ```clojure
  (m-build mpid)
  ;; or
  (workon! mpid)
  ;; followed by
  (m-build)
  ```"
  ([]
   (m-build (deref current-mp-id)))
  ([mp-id]
   (timbre/info "build " mp-id)
   (->> mp-id
        (u/compl-main-path)
        (lt/id->doc)
        (u/doc->safe-doc)
        (build/store))))

(defn m-build-edn
  "Builds up a the mpds in `edn` format provided by *cmp* (see resources
  directory).
  
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
   (d-add (deref current-mp-id) doc-id))
  ([mp-id doc-id]
   (d/add mp-id doc-id)))

(defn d-rm
  "Removes a doc from the api."
  ([doc-id]
   (d-rm (deref current-mp-id) doc-id))
  ([mp-id doc-id]
   (d/rm mp-id doc-id)))

(defn d-ids
  "Gets a list of ids added."
  ([]
   (d-ids (deref current-mp-id)))
  ([mp-id]
   (d/ids mp-id)))

;;------------------------------
;; start observing mp
;;------------------------------
(defn m-start
  "Registers a listener for the `ctrl` interface of a
  `mp-id` (see [[workon!]])."
  ([]
   (m-start (deref current-mp-id)))
  ([mp-id]
   (state/start mp-id)))

;;------------------------------
;; stop observing 
;;------------------------------
(defn m-stop
  "De-registers the listener for the `ctrl` interface of the given
  `mp-id` (see [[workon!]])."
  ([]
   (m-stop (deref current-mp-id)))
  ([mp-id]
   (state/stop mp-id)))

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
  or is derived from `(deref current-mp-id)` [[workon \"ref\"]].


  ```clojure
  (workon! \"ref\"=
  (set-ctrl \"run\")
  ```

  **NOTE:**

  `set-ctrl` only writes to the `container` structure.  The
  `definitions` struct should not be started by a
  user (see [[workon!]])."
  ([i cmd]
   (set-ctrl (deref current-mp-id) i cmd))
  ([mp-id i cmd]
   (st/set-val! (ku/cont-ctrl-key  mp-id (u/lp i)) cmd)))

(defn c-run
  "Shortcut to push a `run` to the control interface of mp container
  `i`."
  [i]
  (set-ctrl (deref current-mp-id) i "run"))

(defn c-mon
  "Shortcut to push a `mon` to the control interface of mp container
  `i`."
  [i]
  (set-ctrl (deref current-mp-id) i "mon"))

(defn c-stop
  "Shortcut to push a `stop` to the control interface of mp container
  `i`."
  [i]
  (set-ctrl (deref current-mp-id) i "stop"))

(defn c-reset
  "Shortcut to push a `reset` to the control interface of mp container
  `i`. The `reset` cmd **don't** de-register the `state` listener so
  that the container starts from the beginning.  **reset is a
  container restart**
  "
  [i]
  (set-ctrl (deref current-mp-id) i "reset"))

(defn c-suspend
  "Shortcut to push a `suspend` to the control interface of mp container
  `i`. The `suspend` cmd de-register the `state` listener and leaves
  the state as it is.
  "
  [i]
  (set-ctrl (deref current-mp-id) i "suspend"))

;;------------------------------
;; tasks
;;------------------------------
(defn t-build
  "Builds the `tasks` endpoint. At runtime all `tasks` are provided by
  `st-mem`. The advantage is: tasks can be modified at runtime." 
  []
  (build/store-tasks (lt/all-tasks)))

(defn t-table
  "Prints a table of **assembled tasks** stored in **short term
  memory**. If a `kw` and `val` are given it is used as a filter

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
   (let [state-key (u/vec->key [mp-id struct (u/lp i) "state" (u/lp j) (u/lp k)])]
     (pp/print-table
      (filter some?
              (mapv (fn [k]
                      (let [name      (ku/key->struct k)
                            meta-task (task/gen-meta-task name)
                            task      (task/assemble meta-task mp-id state-key)
                            value     (kw task)]
                        (if (and value (or (= value v) (= :all v)))
                          {kw value :TaskName name :stm-key k} )))
                    (st/key->keys "tasks")))))))


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
  ```
  "
  ([t]
   (t-run t "core" "test" 0 0 0))
  ([t mp-id]
   (t-run t mp-id "test" 0 0 0))
  ([t mp-id struct no-idx seq-idx par-idx]
   (let [no-idx    (u/lp no-idx)
         seq-idx   (u/lp seq-idx)
         par-idx   (u/lp par-idx)
         func       "response"
         state-key  (u/vec->key [mp-id struct no-idx "state" seq-idx par-idx])
         resp-key   (u/vec->key [mp-id struct no-idx func    seq-idx par-idx])
         meta-task  (task/gen-meta-task t)
         task       (task/assemble meta-task mp-id state-key)]
     (when (task/dev-action? task)
       (timbre/info "task dispached, wait for response...")
       (st/register! mp-id struct no-idx func (fn [msg]
                                                (when-let [result-key (st/msg->key msg)]
                                                  (st/de-register! mp-id struct no-idx func)
                                                  (pp/pprint (st/key->val result-key))))))
     (work/check task))))

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
  ``` 
  "
  [k]
  (let [mp-id     (ku/key->mp-id   k)
        struct    (ku/key->struct  k)
        no-idx    (ku/key->no-idx  k)
        seq-idx   (ku/key->seq-idx k)
        par-idx   (ku/key->par-idx k)
        def-key   (u/vec->key [mp-id struct no-idx "definition" seq-idx par-idx])
        t         (st/key->val def-key)]
    (if t
      (t-run t mp-id struct no-idx seq-idx par-idx)
      (timbre/error (str "no TaskName at key: " k)))))

(defn t-raw
  "Shows the raw task as stored at st-memory" 
  [s]
  (st/key->val (u/vec->key ["tasks" s])))

(defn t-assemble
  "Assembles the task with the given `x` (`TaskName` or `{:TaskName
  \"task-name\" :Replace {:%waittime 3000}}`).  If a `mp-id` (default
  is `\"core\"`) is given `FromExchange` dependencies may be resolved.

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
   (let [i          (u/lp i)
         j          (u/lp j)
         k          (u/lp k)
         state-key  (u/vec->key[mp-id struct i "state" j k])
         meta-task  (task/gen-meta-task x)]
     (task/assemble meta-task mp-id state-key))))
 
(defn t-build-edn
  "Stores the `task` slurped from the files configured in
  `resources/config.edn`.

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
   (m-clear (deref current-mp-id)))
  ([mp-id]
   (m-stop mp-id)
   (st/clean-register! mp-id)
   (st/clear mp-id)))

;;------------------------------
;; p-ubsub events
;;------------------------------
(def p-table (atom []) )
(defn p-start-table
  "Registers a listener. Pretty prints a p-table on events.

  Example:
  ```clojure
  (p-start-table)
  ;; or
  (p-start-table \"ref\")
  ;; or
  (p-start-table \"ref\" \"*\" \"*\" \"state\")
  ```
  Output example:
  ```
  | :h | :m | :s |      :meth |                        :k |     :val |
  |----+----+----+------------+---------------------------+----------|
  | 12 | 54 | 25 | psubscribe |                           |          |
  | 12 | 55 | 21 | psubscribe |                           |          |
  | 12 | 55 | 26 |   pmessage | ref@container@0@state@0@0 |  working |
  | 12 | 55 | 26 |   pmessage | ref@container@0@state@0@1 |  working |
  | 12 | 55 | 29 |   pmessage | ref@container@0@state@0@0 | executed |
  | 12 | 55 | 30 |   pmessage | ref@container@0@state@0@1 | executed |
  | 12 | 55 | 30 |   pmessage | ref@container@0@state@1@0 |  working |
  | 12 | 55 | 30 |   pmessage | ref@container@0@state@1@1 |  working |
  | 12 | 55 | 30 |   pmessage | ref@container@0@state@1@2 |  working |
  | 12 | 55 | 30 |   pmessage | ref@container@0@state@1@3 |  working |
  | 12 | 55 | 33 |   pmessage | ref@container@0@state@1@0 | executed |
  | 12 | 55 | 33 |   pmessage | ref@container@0@state@1@1 | executed |
  | 12 | 55 | 33 |   pmessage | ref@container@0@state@1@2 | executed |
  | 12 | 55 | 34 |   pmessage | ref@container@0@state@1@3 | executed |
  | 12 | 55 | 34 |   pmessage | ref@container@0@state@2@0 |  working |
  | 12 | 55 | 35 |   pmessage | ref@container@0@state@2@0 | executed |
  | 12 | 55 | 35 |   pmessage | ref@container@0@state@0@0 |    ready |
  | 12 | 55 | 35 |   pmessage | ref@container@0@state@0@1 |    ready |
  | 12 | 55 | 35 |   pmessage | ref@container@0@state@1@0 |    ready |
  | 12 | 55 | 35 |   pmessage | ref@container@0@state@1@1 |    ready |
  | 12 | 55 | 35 |   pmessage | ref@container@0@state@1@2 |    ready |
  | 12 | 55 | 35 |   pmessage | ref@container@0@state@1@3 |    ready |
  | 12 | 55 | 35 |   pmessage | ref@container@0@state@2@0 |    ready |
  ```
  "
  ([]
   (p-start-table "*" "*" "*" "*"))
  ([mp-id struct]
   (p-start-table mp-id struct "*" "*"))
  ([mp-id struct i]
   (p-start-table mp-id struct i "*"))
  ([mp-id struct i func]
   (let [i   (u/lp i)
         cb! (fn [msg]
               (let [d   (u/get-date-object)
                     k   (st/msg->key msg)
                     val (st/key->val k)]
                 (swap! p-table conj {:h    (u/get-hour d)
                                      :m    (u/get-min d)
                                      :s    (u/get-sec d)
                                      :meth (nth msg 0)
                                      :k    k
                                      :val  val })
                 (pp/print-table (deref p-table))))]
     (st/register! mp-id struct i func cb!))))

(defn p-clear-table
  []
  "Resets the p-table `atom`."
  (reset! p-table []))

(defn p-stop-table
  "De-registers the pubsub listener.  Resets the p-table `atom`."
  ([]
   (p-stop-table "*" "*" "*" "*"))
  ([mp-id struct]
   (p-stop-table mp-id struct "*" "*"))
  ([mp-id struct i]
   (p-stop-table mp-id struct i "*"))
  ([mp-id struct i func]
   (p-clear-table)
   (st/de-register! mp-id struct (u/lp i) func)))

;;------------------------------
;; Exchange table
;;------------------------------
(defn e-table
  "Pretty prints a key-value table of the `exchange`-interface of the
  mpd with the id `mp-id`.

  Example output:
  ```
  |                                :key |                                            :value |
  |-------------------------------------+---------------------------------------------------|
  |          se3-cmp_valves@exchange@V1 |                                         {:Bool 1} |
  |         se3-cmp_valves@exchange@V10 |                                         {:Bool 1} |
  |         se3-cmp_valves@exchange@V11 |                                         {:Bool 0} |
  |         se3-cmp_valves@exchange@V12 |                                         {:Bool 0} |
  |         se3-cmp_valves@exchange@V13 |                                         {:Bool 1} |
  |         se3-cmp_valves@exchange@V14 |                                         {:Bool 1} |
  |         se3-cmp_valves@exchange@V15 |                                         {:Bool 0} |
  |         se3-cmp_valves@exchange@V16 |                                         {:Bool 0} |
  |         se3-cmp_valves@exchange@V17 |                                         {:Bool 1} |
  |         se3-cmp_valves@exchange@V18 |                                         {:Bool 1} |
  |         se3-cmp_valves@exchange@V19 |                                         {:Bool 0} |
  |          se3-cmp_valves@exchange@V2 |                                         {:Bool 1} |
  |         se3-cmp_valves@exchange@V20 |                                         {:Bool 0} |
  |          se3-cmp_valves@exchange@V3 |                                         {:Bool 1} |
  |          se3-cmp_valves@exchange@V4 |                                         {:Bool 1} |
  |          se3-cmp_valves@exchange@V5 |                                         {:Bool 0} |
  |          se3-cmp_valves@exchange@V6 |                                         {:Bool 1} |
  |          se3-cmp_valves@exchange@V7 |                                         {:Bool 0} |
  |          se3-cmp_valves@exchange@V8 |                                         {:Bool 0} |
  |          se3-cmp_valves@exchange@V9 |                                         {:Bool 1} |
  | se3-cmp_valves@exchange@Vraw_block1 | [1 0 1 0 1 0 1 0 0 0 1 0 0 0 0 0 0 0 1 0 1 0 0 0] |
  | se3-cmp_valves@exchange@Vraw_block2 | [0 0 1 0 0 0 0 0 0 0 1 0 1 0 0 0 1 0 1 0 0 0 0 0] |
  | se3-cmp_valves@exchange@Vraw_block3 | [0 0 1 0 1 0 0 0 1 0 1 0 0 0 0 0 1 1 0 0 0 0 0 0] |
  | se3-cmp_valves@exchange@Vraw_block4 | [1 0 1 0 0 0 0 0 1 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0] |
  | se3-cmp_valves@exchange@Vraw_block5 | [1 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0] |
  ```
  "
  ([]
   (e-table  (deref current-mp-id)))
  ([mp-id]
   (pp/print-table
    (mapv (fn [k]
            {:key k :value (st/key->val k)})
          (st/key->keys (ku/exch-prefix mp-id))))))

