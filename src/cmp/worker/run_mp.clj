(ns cmp.worker.run-mp
  ^{:author "wactbprot"
    :doc "run-mp worker."}
  (:require [cmp.config              :as cfg]
            [cmp.key-utils           :as ku]
            [com.brunobonacci.mulog  :as mu]
            [cmp.st-mem              :as st]
            [cmp.utils               :as u]))

(defn exec-index
  "Registers a level b callback for the `i`th container of the mpd `mp`."
  [{mp :Mp i :Container state-key :StateKey cmd :Cmd}]
  (let [mp       (u/extr-main-path mp)
        cmd      (keyword (or cmd "run"))
        ctrl-key (ku/cont-ctrl-key mp i)
        func     "ctrl"
        struct   "container"
        level    "b"
        f        (fn [msg]
                   (when (st/msg->key msg)
                      (condp = (keyword (st/key->val ctrl-key))
                        :ready (do
                                 (mu/log ::exec-index :message "ready callback for" :key ctrl-key)
                                 (st/set-state! state-key :executed)
                                 (mu/log ::exec-index :message "set executed" :key state-key)
                                 (st/de-register! mp struct i func level)
                                 (mu/log ::exec-index :message "de-registered"))
                        :error (do
                                 (mu/log ::exec-index :error "error callback for" :key ctrl-key)
                                 (st/set-state! state-key :error))
                        (mu/log ::exec-index :message "run callback not :ready no :error" :key ctrl-key))))]
    (st/register! mp struct i func f level)
    (st/set-state! ctrl-key cmd)))

(defn exec-title
  "Searches for the given  `:ContainerTitle`. Extracts the `no-idx`
  and uses the `exec-index` function to register a callback."
  [{mp :Mp cont-title :ContainerTitle state-key :StateKey cmd :Cmd}]
  (let [mp     (u/extr-main-path mp)
        ks     (st/pat->keys (ku/cont-title-key mp "*" ))
        title? (fn [k] (= cont-title (st/key->val k)))]
    (if-let [k (first (filter title? ks))]
      (exec-index {:Mp mp :Container (ku/key->no-idx k) :StateKey state-key :Cmd cmd}) 
      (st/set-state! state-key :error (str "no container with title: >"cont-title "<")))))

(defn run-mp!
  "Runs a certain container of a `mpd`. `:ContainerTitle` is prefered
  over `:Container` if both are given. The `task` is marked as
  `:executed` if all tasks in the container are executed."
  [task]
  (let [{title :ContainerTitle index :Container state-key :StateKey} task]
    (st/set-state! state-key :working)
    (cond
      title (exec-title task)
      index (exec-index task)
      :not-found (st/set-state! state-key :error "neither title nor index"))))
