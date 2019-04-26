(ns cmp.lt
  (:require [com.ashafa.clutch :as couch]
            [taoensso.timbre :as log])
  (:use [clojure.repl]);; enables e.g. (doc .)
  (:gen-class))

(log/set-level! :debug)

(def conn "http://localhost:5984/vl_db")

(defn get-document [id]
  (log/debug "document id is: " id)
  (couch/get-document conn id))

(defn get-task-view [{task-name :TaskName}]
  (log/debug "task name is: " task-name)
  (first (couch/get-view conn "dbmp" "tasks" {:key task-name})))