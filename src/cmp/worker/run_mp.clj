(ns cmp.worker.run-mp
  ^{:author "wactbprot"
    :doc "run-mp worker."}
  (:require [taoensso.timbre :as log]
            [cmp.st-mem :as st]
            [cmp.utils :as u]
            [cmp.config :as cfg]))

(def mtp (cfg/min-task-period (cfg/config)))

(defn exec-index
  "Registers a callback for the `i`th container of the mpd `mp`."
  [{mp :Mp  i :Container state-k :StateKey}]
  (let [ctrl-k (st/cont-ctrl-path mp i)]
    (st/register! mp "container" i "definition" (st/listener-callback ctrl-k state-k))
    (st/set-val! ctrl-k "run")))

(defn exec-title
  "Searches for the given  `:ContainerTitle`. Extracts the `no-idx`
  and uses the `exec-index` function to register a callback."
  [{mp :Mp cont-title :ContainerTitle state-key :StateKey}]
  (let [ks (st/pat->keys (u/vec->key [mp "container" "*" "title"]))
        title? (fn [k] (= cont-title (st/key->val k)))]
    (if-let [k (first (filter title? ks))]
      (exec-index {:Mp mp  :Container (st/key->no-idx k) :StateKey state-key}) 
      (do
        (log/error "no container with title: " cont-title)
        (st/set-val! state-key "error")))))

(defn run-mp!
  "Runs a certain container of a mpd. Task is marked as executed if all
  tasks in the container are executed."
  [task]
  (let [{cont-title :ContainerTitle
         cont-index :Container
         state-key  :StateKey} task]
    (when state-key
      (st/set-val! state-key "working")
      (log/debug "start with wait, already set " state-key  " working"))
    (cond
      (not (nil? cont-title)) (exec-title task)
      (not (nil? cont-index)) (exec-index task)
      :not-found (when state-key
                   (log/error (str "no container title or index at: " state-key))
                   (st/set-val! state-key "error")))))