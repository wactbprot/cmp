(ns cmp.config
  (:require [aero.core :as aero]))

(defn config
  []
  (aero/read-config  "resources/config.edn"))

(defn lt-conn
  [c]
  (:lt-uri c))

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

(defn post-header
  [c]
  (:post-header c))

(defn dev-hub-url
  [c]
  (:dev-hub-url c))

