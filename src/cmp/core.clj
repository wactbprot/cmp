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
  @current-mp
  ```"
  [mp-id]
  (reset! current-mp mp-id))

;;------------------------------
;; info
;;------------------------------
(defn m-data
  "Returns a info map about the mpd with the id `mp-id`."
  ([]
   (m-data @current-mp))
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
        (st/pat->keys (ku/meta-name-key "*"))))

(defn c-data
  "Returns a map about the `i`th container of the mpd with the id
  `mp-id`."
  ([]
   (c-data @current-mp 0))
  ([i]
   (c-data @current-mp i))
  ([mp-id i]
   (let [idx      (u/lp i)
         state-ks (st/key->keys (ku/cont-state-key mp-id idx))
         defin-ks (st/key->keys (ku/cont-defin-key mp-id idx))]
     (mapv (fn [sk dk] {:state-val (st/key->val sk)
                        :state-key sk
                        :defin-val (st/key->val dk)
                        :defin-key dk})
           state-ks defin-ks))))
  
(defn n-data
  "Returns a map about the `i`th defi**n**itions of the mpd with the id
  `mp-id`."
  ([]
   (c-data @current-mp 0))
  ([i]
   (c-data @current-mp i))
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
   (cs-data @current-mp))
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
   (m-start @current-mp))
  ([mp-id]
   (state/start mp-id)))

;;------------------------------
;; stop observing
;;------------------------------
(defn m-stop
  "De-registers the listener for the `ctrl` interface of the given
  `mp-id` (see [[workon!]])."
  ([]
   (m-stop @current-mp))
  ([mp-id]
   (state/stop mp-id)))

;;------------------------------
;; build mpd
;;------------------------------
(defn m-build
  "Loads a mpd from long term memory and builds up a `st-mem` version of
  it. The `mp-id` may be set with [[workon!]]. [[m-start]] is called
  after mp is build.

  Example:
  ```clojure
  (m-build mpid)
  ;; or
  (workon! mpid)
  ;; followed by
  (m-build)
  ```"
  ([]
   (m-build @current-mp))
  ([mp-id]
   (println "stop " mp-id)
   (m-stop mp-id)
   (println "clear " mp-id)
   (st/clear! mp-id)
   (println "build " mp-id)
   (->> mp-id u/compl-main-path lt/get-doc u/doc->safe-doc build/store)
   (println "start " mp-id)
   (m-start mp-id)))

(defn m-build-ref
  "Builds up the `ref`erence mpd provided in `edn` format in the
  resources folder."
  []
  (println "try to slurp and build ref")
  (let [mp    (-> (cfg/ref-mpd (cfg/config)) slurp read-string)
        mp-id (u/extr-main-path (:_id mp))]
    (println "stop " mp-id)
    (m-stop mp-id)
    (println "clear " mp-id)
    (st/clear! mp-id)
    (println "build " mp-id)
    (build/store mp)
    (workon! mp-id)
    (println "start " mp-id)
    (m-start)))

;;------------------------------
;; documents
;;------------------------------
(defn d-add
  "Adds a doc to the api to store the resuls in."
  ([doc-id]
   (d-add @current-mp doc-id))
  ([mp-id doc-id]
   (d/add mp-id doc-id)))

(defn d-rm
  "Removes a doc from the api."
  ([doc-id]
   (d-rm @current-mp doc-id))
  ([mp-id doc-id]
   (d/rm mp-id doc-id)))

(defn d-ids
  "Gets a list of ids added."
  ([]
   (d-ids @current-mp))
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
  or is derived from `@current-mp` [[workon \"ref\"]].


  ```clojure
  (workon! \"ref\"=
  (set-ctrl \"run\")
  ```

  **NOTE:**

  `set-ctrl` only writes to the `container` structure.  The
  `definitions` struct should not be started by a
  user (see [[workon!]])."
  ([i cmd]
   (set-ctrl @current-mp i cmd))
  ([mp-id i cmd]
   (st/set-val! (ku/cont-ctrl-key mp-id (u/lp i)) cmd)))

(defn c-run
  "Shortcut to push a `run` to the control interface of mp container
  `i`."
  [i]
  (set-ctrl @current-mp i "run"))

(defn c-mon
  "Shortcut to push a `mon` to the control interface of mp container
  `i`."
  [i]
  (set-ctrl @current-mp i "mon"))

(defn c-stop
  "Shortcut to push a `stop` to the control interface of mp container
  `i`."
  [i]
  (set-ctrl @current-mp i "stop"))

(defn c-reset
  "Shortcut to push a `reset` to the control interface of mp container
  `i`. The `reset` cmd **don't** de-register the `state` listener so
  that the container starts from the beginning.  **reset is a
  container restart**"
  [i]
  (set-ctrl @current-mp i "reset"))

(defn c-suspend
  "Shortcut to push a `suspend` to the control interface of mp container
  `i`. The `suspend` cmd de-register the `state` listener and leaves
  the state as it is."
  [i]
  (set-ctrl @current-mp i "suspend"))

;;------------------------------
;; tasks
;;------------------------------
(defn t-build
  "Builds the `tasks` endpoint. At runtime all `tasks` are provided by
  `st-mem`. The advantage is: tasks can be modified at runtime."
  []
  (build/store-tasks (lt/all-tasks)))

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
   (m-clear @current-mp))
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
   (e-data  @current-mp))
  ([mp-id]
   (mapv (fn [k]
           {:key k :value (st/key->val k)})
         (st/key->keys (ku/exch-prefix mp-id)))))
