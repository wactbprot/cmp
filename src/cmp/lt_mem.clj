(ns cmp.lt-mem
  (:require [clojure.string :as string]
            [com.ashafa.clutch :as couch]
            [taoensso.timbre :as timbre]
            [cmp.config :as cfg])
  (:use [clojure.repl]))

(def conn (cfg/lt-conn (cfg/config)))

(defn id->doc
  [id]
  (timbre/debug "try to get document with id: " id)
  (try
    (couch/get-document conn id)
    (catch Exception e
      (timbre/error (.getMessage e)))))

(defn get-task-view
  [{task-name :TaskName}]
  (timbre/debug "get task: " task-name " from ltm")
  (first
   (couch/get-view conn "dbmp" "tasks" {:key task-name})))

(defn get-all-tasks
  []
  (timbre/debug "get tasks from ltm")
  (couch/get-view conn "dbmp" "tasks"))

(defn doc->version
  [{rev :_rev}]
  (first (string/split rev  #"-")))

(defn doc->id
  [{id :_id}]
  id)
