(ns cmp.lt
  (:require [clojure.string :as string]
            [com.ashafa.clutch :as couch]
            [taoensso.timbre :as timbre]
            [cmp.config :as cfg])
  (:use [clojure.repl])
  (:gen-class))


(def conn (cfg/lt-conn (cfg/config)))

(defn get-doc
  [id]
  (timbre/debug "try to get document with id: " id)
  (try
    (couch/get-document conn id)
    (catch Exception ex
      (timbre/error (.getMessage ex))
      nil)))

(defn get-task-view
  [{task-name :TaskName}]
  (timbre/debug "get task: " task-name " from ltm")
  (first
   (couch/get-view conn "dbmp" "tasks" {:key task-name})))

(defn get-doc-version
  [{rev :_rev}]
  (first (string/split rev  #"-")))

(defn get-doc-id
  [{id :_id}]
  id)
