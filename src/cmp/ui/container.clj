(ns cmp.ui.container
  (:require
     [cmp.ui.core :as ui]))

(defn view-ctrl
  [conf data mp]
  (ui/index conf (ui/table conf data [:key :mp-id :struct :func :no-idx :value :run :stop :mon]) mp))

(defn view-state
  [conf data mp]
  (ui/index conf (ui/table conf data [:key :mp-id :struct :func :no-idx :value :ready :working :executed]) mp))

(defn view
  [conf data mp]
  (ui/index conf (ui/table conf data [:key :mp-id :struct :func :no-idx :value]) mp))
