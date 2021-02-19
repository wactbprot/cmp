(ns cmp.api
      ^{:author "wactbprot"
        :doc "api for cmp info and ctrl."}
  (:require [cmp.build               :as build]
            [cheshire.core           :as che]
            [cmp.config              :as config]
            [cmp.exchange            :as exch]
            [cmp.doc                 :as doc]
            [cmp.lt-mem              :as lt]
            [cmp.st-mem              :as st]
            [cmp.state               :as state]
            [cmp.task                :as tsk]
            [cmp.utils               :as u]
            [cmp.api-utils           :as au]
            [cmp.key-utils           :as ku]
            [com.brunobonacci.mulog  :as mu]))

(comment
  (def conf (config/config)))

;;------------------------------
;; start observing mp
;;------------------------------
(defn m-start
  "Registers a listener for the `ctrl` interface of a
  `mp-id` (see [[workon!]])."
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
   (->> mp-id u/compl-main-path lt/get-doc u/doc->safe-doc build/store)
   (m-start conf mp-id)))

;;------------------------------
;; build ref mpd
;;------------------------------
(defn m-build-ref
  "Builds up the `ref`erence mpd provided in `edn` format in the
  resources folder."
  [conf]
  (let [doc (-> (config/ref-mpd conf) slurp read-string)
        mp-id (u/extr-main-path (:_id doc))]
    (m-stop conf mp-id)
    (st/clear! mp-id)
    (build/store doc)
    (m-start conf mp-id)))

;;------------------------------
;; listeners 
;;------------------------------
(defn listeners
  "Returns the `reg-key` and the `id` of the running listeners.

  Example:
  ```clojure
  (listener {} {})
  ```"
  [conf req]
  (let [ls @st/listeners]
    (mapv (fn [k] (assoc (ku/key->reg-map k) :key k :listener-id (get-in ls [k :id]))) (keys ls))))

;;------------------------------
;; tasks
;;------------------------------
(defn tasks
  "Returns the `tasks` `key`-`value` pairs available at `st-mem`.

  Example:
  ```clojure
  (tasks {} {})
  ```"
  [conf req]
  (mapv au/key-value-map (st/key->keys (ku/task-prefix))))

(defn t-build
  "Builds the `tasks` endpoint. At runtime all `tasks` are provided by
  `st-mem`. The advantage is: tasks can be modified at runtime."
  [conf]
  (build/store-tasks (lt/all-tasks)))

(defn t-clear
  "Function removes all keys starting with `tasks`."
  [conf]
  (st/clear! (ku/task-prefix)))

(defn t-refresh
  "Refreshs the `tasks` endpoint.

  Example:
  ```clojure
  (t-refresh {})
  ```"
  [conf]
  (t-clear conf)
  (t-build conf))

;;------------------------------
;; mp info
;;------------------------------
(defn mp-meta
  [conf req]
  (let [mp-id (au/req->mp-id req)]
    {:mp-id mp-id
     :descr   (st/key->val (ku/meta-descr-key   mp-id))
     :name    (st/key->val (ku/meta-name-key    mp-id))
     :ncont   (st/key->val (ku/meta-ncont-key   mp-id))
     :ndefins (st/key->val (ku/meta-ndefins-key mp-id))
     :std     (st/key->val (ku/meta-std-key     mp-id))
     :docs    (mapv st/key->val (st/key->keys (ku/id-prefix mp-id)))}))

;;------------------------------
;; container
;;------------------------------

(defn container-ctrl 
  [conf req]
  (let [mp-id   (au/req->mp-id req)
        no-idx  (au/req->no-idx req)]
    (mapv (fn [ck tk] (au/key-value-map ck {:run   "run"
                                            :mon   "mon"
                                            :stop  "stop"
                                            :title (st/key->val tk)}))
          (st/pat->keys (ku/cont-ctrl-key mp-id no-idx))
          (st/pat->keys (ku/cont-title-key mp-id no-idx)))))

(defn container-state
  [conf req]
  (let [mp-id   (au/req->mp-id req)
        no-idx  (au/req->no-idx req)]
    (mapv
     (fn [k] (au/key-value-map k {:ready    "ready"
                                  :working  "working"
                                  :executed "executed"
                                  :title    (st/key->val (ku/cont-title-key mp-id no-idx))}))
     (st/pat->keys (ku/cont-state-key mp-id no-idx "*" "*" )))))

(defn container-definition
  [conf req]
  (let [mp-id   (au/req->mp-id req)
        no-idx  (au/req->no-idx req)]
    (mapv (fn [k] (au/key-value-map k {:task  (tsk/build k)}))
          (st/pat->keys (ku/cont-defin-key mp-id no-idx "*" "*" )))))


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
;; set value to st-mem
;;------------------------------
(defn set-val!
  [conf req]
  (let [k (get-in req [:body :key])
        v (get-in req [:body :value])]
    (if (and k v)
      (if (= "OK" (st/set-val! k v))
        {:ok true}
        {:error "on attempt to set value"}) 
      {:error "missing key or value"})))

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
  (st/set-val! (ku/cont-ctrl-key mp-id (u/lp i)) cmd))

(defn c-run
  "Shortcut to push a `run` to the control interface of mp container
  `i`."
  [conf mp-id i]
  (set-ctrl @current-mp i "run"))

(defn c-mon
  "Shortcut to push a `mon` to the control interface of mp container
  `i`."
  [conf mp-id i]
  (set-ctrl mp-id i "mon"))

(defn c-stop
  "Shortcut to push a `stop` to the control interface of mp container
  `i`."
  [conf mp-id i]
  (set-ctrl mp-id i "stop"))

(defn c-reset
  "Shortcut to push a `reset` to the control interface of mp container
  `i`. The `reset` cmd **don't** de-register the `state` listener so
  that the container starts from the beginning.  **reset is a
  container restart**"
  [conf mp-id i]
  (set-ctrl mp-id i "reset"))

(defn c-suspend
  "Shortcut to push a `suspend` to the control interface of mp container
  `i`. The `suspend` cmd de-register the `state` listener and leaves
  the state as it is."
  [conf mp-id i]
  (set-ctrl mp-id i "suspend"))
