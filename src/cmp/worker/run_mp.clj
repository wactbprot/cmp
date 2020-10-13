(ns cmp.worker.run-mp
  ^{:author "wactbprot"
    :doc "run-mp worker."}
  (:require [taoensso.timbre :as log]
            [cmp.st-mem :as st]
            [cmp.utils :as u]
            [cmp.config :as cfg]))

(defn exec-index
  "Registers a level b callback for the `i`th container of the mpd `mp`."
  [{mp :Mp  i :Container state-k :StateKey}]
  (let [mp        (u/main-path mp)
        ctrl-k    (st/cont-ctrl-path mp i)
        func      "ctrl"
        struct    "container"
        level     "b"
        callback  (fn [msg]
                    (condp = (keyword (st/key->val ctrl-k))
                      :ready (do
                               (log/debug "ready callback for" ctrl-k)
                               (st/set-state! state-k :executed)
                               (log/debug "set" state-k " to executed" )
                               (st/de-register! mp struct i func level)
                               (log/debug "de-registered" mp struct i func level ))
                      :error (do
                               (log/error "error callback for" ctrl-k)
                               (st/set-state! state-k :error))
                      (log/debug "run callback for" ctrl-k)))]
    (st/register! mp struct i func callback level)
    (st/set-state! ctrl-k :run)))

(defn exec-title
  "Searches for the given  `:ContainerTitle`. Extracts the `no-idx`
  and uses the `exec-index` function to register a callback."
  [{mp :Mp cont-title :ContainerTitle state-key :StateKey}]
  (let [mp     (u/main-path mp)
        ks     (st/pat->keys (st/cont-title-path mp "*" ))
        title? (fn [k] (= cont-title (st/key->val k)))]
    (if-let [k (first (filter title? ks))]
      (exec-index {:Mp mp
                   :Container (st/key->no-idx k)
                   :StateKey state-key}) 
      (do
        (log/error (str "no container with title: >"cont-title "<"))
        (st/set-state! state-key :error)))))

(defn run-mp!
  "Runs a certain container of a mpd. Task is marked as executed if all
  tasks in the container are executed."
  [task]
  (let [{cont-title :ContainerTitle
         cont-index :Container
         state-key  :StateKey} task]
    (st/set-state! state-key :working)
    (cond
      (not (nil? cont-title)) (exec-title task)
      (not (nil? cont-index)) (exec-index task)
      :not-found (st/set-state! state-key :error))))
