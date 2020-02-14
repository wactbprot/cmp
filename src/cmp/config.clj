(ns cmp.config
  (:require [aero.core :as aero]))

(defn config
  []
  (aero/read-config  "resources/config.edn"))

(defn lt-conn
  [c]
  (c :lt-uri))

(defn st-conn
  [c]
  (c :st-conn))
