(ns cmp.config
  (:require [aero.core :as aero]))

(defn config
  []
  (aero/read-config (clojure.java.io/resource "config.edn")))

(defn lt-conn
  [config]
  (config :lt-uri))

(defn st-conn
  [config]
  (config :st-conn))