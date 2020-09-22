(ns cmp.config
  (:require [aero.core :as aero]))

(defn config
  []
  (aero/read-config  "resources/config.edn"))

(defn lt-conn
  [c]
  (let [usr (System/getenv "CAL_USR")
        pwd (System/getenv "CAL_PWD")]
    (if (and usr pwd)
      (str (:lt-prot c)"://"usr":"pwd"@"(:lt-srv c)":"(:lt-port c)"/"(:lt-db c))
      (str (:lt-prot c)"://"(:lt-srv c)":"(:lt-port c)"/"(:lt-db c)))))

(defn st-conn
  [c]
  (:st-conn c))

(defn st-db
  [c]
  (get-in c [:st-conn :spec :db]))

(defn edn-tasks
  [c]
  (:edn-tasks c))

(defn edn-mpds
  [c]
  (:edn-mpds c))

(defn min-task-period
  [c]
  (:min-task-period c))

(defn dev-hub-post-header
  [c]
  (:dev-hub-post-header c))

(defn anselm-post-header
  [c]
  (:anselm-post-header c))

(defn dev-hub-url
  [c]
  (:dev-hub-url c))

(defn anselm-url
  [c]
  (:anselm-url c))
