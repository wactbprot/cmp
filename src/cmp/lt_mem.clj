(ns cmp.lt-mem
  (:require [clojure.string :as string]
            [com.ashafa.clutch :as couch]
            [cmp.config :as cfg]
            [taoensso.timbre :as log]))

(defn id->doc
  "Gets a document from the long term memory."
  [id]
  (log/debug "try to get document with id: " id)
  (try
    (couch/get-document (cfg/lt-conn (cfg/config)) id)
    (catch Exception e
      (log/error "catch error on attempt to get doc: " id))))

(defn put-doc
  "Saves a document to the long term memory."
  [doc]
  (log/debug "try to save document with id: " (:_id doc))
  (try
    (couch/put-document (cfg/lt-conn (cfg/config)) doc)
    (catch Exception e
      (log/error "catch error on attempt to put doc"))))

(defn all-tasks
  "Returns all tasks."
  []
  (log/debug "get tasks from ltm")
  (couch/get-view (cfg/lt-conn (cfg/config)) "dbmp" "tasks"))
