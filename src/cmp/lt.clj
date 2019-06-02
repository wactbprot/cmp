(ns cmp.lt
  (:require [clojure.string :as string]
            [com.ashafa.clutch :as couch]
            [taoensso.timbre :as log])
  (:use [clojure.repl])
  (:gen-class))

(log/set-level! :debug)

(def conn "http://localhost:5984/vl_db")

(defn get-doc
  [id]
  (log/debug "document id to get is: " id)
  (couch/get-document conn id))

(defn get-task-view
  [{task-name :TaskName}]
  (log/debug "task name is: " task-name)
  (first
   (couch/get-view conn "dbmp" "tasks" {:key task-name})))

(defn get-doc-version
  [{rev :_rev}]
  (first (string/split rev  #"-")))

(defn get-doc-id
  [{id :_id}]
  id)
