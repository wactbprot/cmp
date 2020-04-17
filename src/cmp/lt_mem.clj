(ns cmp.lt-mem
  (:require [clojure.core.async :as a]
            [clojure.string :as string]
            [com.ashafa.clutch :as couch]
            [cmp.config :as cfg]
            [cmp.excep :as excep]
            [taoensso.timbre :as timbre]))

(def conn (cfg/lt-conn (cfg/config)))

(defn id->doc
  "Gets a document from the long term memory."
  [id]
  (timbre/debug "try to get document with id: " id)
  (try
    (couch/get-document conn id)
    (catch Exception e
      (timbre/error "catch error on attempt to get doc: " id)
      (a/>!! excep/ch e))))

(defn put-doc
  "Saves a document to the long term memory."
  [doc]
  (timbre/debug "try to save document with id: " (:_id doc))
  (try
    (couch/put-document conn doc)
    (catch Exception e
      (timbre/error "catch error on attempt to put doc")
      (a/>!! excep/ch e))))

(defn get-all-tasks
  "Returns all tasks."
  []
  (timbre/debug "get tasks from ltm")
  (couch/get-view conn "dbmp" "tasks"))
