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

(defn extr-doc-type
  "Extracts the document type.
  Assumes the type of the document to be the
  first key hierarchy beside _id and :rev"
  [doc]
  (first
   (filter
    (fn [kw] (not
              (or
               (= :_id kw)
               (= :_rev kw))))
    (keys doc))))

(defmulti extr-info
  extr-doc-type)

(defmethod extr-info :Calibration
  [doc]
  {:doc-version (get-doc-version doc)
   :doc-id (get-doc-id doc)
   :doc-type "Calibration"})

(defmethod extr-info :Measurement
  [doc]
  {:doc-version (get-doc-version doc)
   :doc-id (get-doc-id doc)
   :doc-type "Measurement"})

(defmethod extr-info :State
  [doc]
  {:doc-version (get-doc-version doc)
   :doc-id (get-doc-id doc)
   :doc-type "State"})

(defmethod extr-info :default
  [doc]
  {:doc-version (get-doc-version doc)
   :doc-id (get-doc-id doc)
   :doc-type "default"})
