(ns cmp.worker.ctrl-mp
  ^{:author "wactbprot"
    :doc "run-mp worker."} 
  (:require [com.brunobonacci.mulog  :as mu]
            [cmp.st-mem              :as st]
            [cmp.st-utils            :as stu]
            [cmp.utils               :as u]))

(defn title->no-idx
  [mp title]
  (let [mp     (u/extr-main-path mp)
        ks     (st/pat->keys (stu/cont-title-key mp "*" ))
        title? (fn [k] (= title (st/key->val k)))]
    (stu/key->no-idx (first (filter title? ks)))))

(defn gen-callback
  [{mp :Mp no-idx :Container state-key :StateKey cmd :Cmd}]
  (let [mp       (u/extr-main-path mp)
        ctrl-key (stu/cont-ctrl-key mp no-idx)]
    (fn [msg]
      (when (st/msg->key msg)
        (let [cmd (keyword (st/key->val ctrl-key))]  
          (cond
            (or 
             (= cmd :ready)
             (= cmd :stop)) (do
                              (st/set-state! state-key :executed)
                              (st/de-register! mp "container" no-idx "ctrl" "b"))
            (= cmd :error)  (do
                              (mu/log ::exec-index :error "error callback for" :key ctrl-key)
                              (st/set-state! state-key :error))
            :default        (mu/log ::exec-index :message "run callback not :ready no :error"
                                    :key ctrl-key)))))))

(defn exec-index
  "Registers a level b callback for the `i`th container of the mpd `mp`."
  [{mp :Mp no-idx :Container state-key :StateKey cmd :Cmd :as task}]
  (let [mp       (u/extr-main-path mp)
        cmd      (keyword (or cmd "run"))
        ctrl-key (stu/cont-ctrl-key mp no-idx)]
    (st/register! mp "container" no-idx "ctrl" (gen-callback task) "b")
    (st/set-state! ctrl-key cmd)))

(defn exec-title
  "Searches for the given  `:ContainerTitle`. Extracts the `no-idx`
  and uses the `exec-index` function to register a callback."
  [{mp :Mp cont-title :ContainerTitle state-key :StateKey cmd :Cmd}]
  (if-let [no-idx (title->no-idx mp cont-title )]
    (exec-index {:Mp mp :Container no-idx :StateKey state-key :Cmd cmd}) 
    (st/set-state! state-key :error (str "no container with title: >"cont-title "<"))))

(defn run-mp!
  "Runs a certain container of a `mpd`. `:ContainerTitle` is prefered
  over `:Container` if both are given. The `task` is marked as
  `:executed` if all tasks in the container are executed."
  [{title :ContainerTitle index :Container state-key :StateKey :as task}]
  (st/set-state! state-key :working)
  (cond
    title (exec-title task)
    index (exec-index task)
    :not-found (st/set-state! state-key :error "neither title nor index")))

(defn stop-mp!
  "Stops a certain container of a `mpd`. `:ContainerTitle` is prefered
  over `:Container` if both are given. Checks if the container to stop
  is the `same?` as the task runs in:

  * If so: the `ctrl` interface is set to `stop` (and nothing
  else). The stop process turns all states to `ready`.
  * If not: the task (resp. the :value of `:StateKey`) is set to
  `:executed` after  stopping."
  [{mp :Mp title :ContainerTitle index :Container state-key :StateKey :as task}]
  (st/set-state! state-key :working)
  (let [mp-id    (u/extr-main-path mp)
        ctrl-key (cond
                   title (stu/cont-ctrl-key mp-id (title->no-idx mp-id title))
                   index (stu/cont-ctrl-key mp-id index))
        same?    (= ctrl-key (stu/key->ctrl-key state-key))]
    (st/set-val! ctrl-key "stop")
    (Thread/sleep 100)
    (when-not same? (st/set-state! state-key :executed))))


