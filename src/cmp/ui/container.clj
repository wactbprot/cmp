(ns cmp.ui.container
  (:require
     [cmp.ui.core :as ui]))

(defn view-ctrl
  [conf data]
  (ui/index conf (ui/table conf data [:key :mp-id :struct :func :no-idx :value :run :stop :mon])))

(defn view-state
  [conf data]
  (ui/index conf (ui/table conf data [:key :mp-id :struct :func :no-idx :value :ready :working :executed])))

(defn view
  [conf data]
  (ui/index conf (ui/table conf data [:key :mp-id :struct :func :no-idx :value])))
