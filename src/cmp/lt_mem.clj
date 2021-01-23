(ns cmp.lt-mem
  (:require [cmp.config              :as cfg]
            [com.ashafa.clutch       :as couch]
            [com.brunobonacci.mulog  :as mu]
            [clojure.string          :as string]))

(def conn (cfg/lt-conn (cfg/config)))

;;------------------------------
;; get doc
;;------------------------------
(defn id->doc
  "Gets a document from the long term memory."
  [id]
  (mu/log ::id->doc :message "try to get document" :doc-id id)
  (try
    (couch/get-document conn id)
    (catch Exception e (mu/log ::id->doc :error (.getMessage e) :doc-id id))))

;;------------------------------
;; put doc
;;------------------------------
(defn put-doc
  "Puts a document to the long term memory."
  [doc]
  (mu/log ::id->doc :message "try to put document" :doc-id (:_id doc))
  (try
    (couch/put-document conn doc)
    (catch Exception e (mu/log ::id->doc :error (.getMessage e) :doc-id (:_id doc)))))

;;------------------------------
;; tasks
;;------------------------------
(defn all-tasks
  "Returns all tasks.
  
  TODO: view names `dbmp` and `tasks` --> conf"
  []
  (mu/log ::all-tasks :message "get tasks from ltm")
  (couch/get-view conn "dbmp" "tasks"))

;;------------------------------
;; utils
;;------------------------------
(defn exist?
  "Returns `true` if a document with the `id` exists.

  TODO: HEAD request not entire doc
  
  Example:
  ```clojure
  (exist? \"foo-bar\")
  ;; =>
  ;; false
  ```"
  [id]
  (map? (id->doc id)))
  
(defn rev-refresh
  "Refreshs the revision `_rev` of the document if
  it exist."
  [doc]
  (if-let [db-doc (id->doc (:_id doc))] 
    (assoc doc :_rev (:_rev db-doc))
    doc))

