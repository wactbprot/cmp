(ns cmp.ui.container
  (:require
     [cmp.ui.core :as ui]))

(defn view-ctrl
  [conf req data]
  (ui/index conf req
            (ui/table conf data [:key  :struct :func :no-idx :title :value :run :stop :mon])))

(defn view-state
  [conf req data]
  (ui/index conf req
            (into [:div]
                  [[:h3 {:class "title"} (:title (first data))]
                   (ui/table conf data [:key  :struct :func :no-idx :seq-idx :par-idx :value :ready :working :executed])])))

(defn view
  [conf req data]
  (ui/index conf req
            (ui/table conf data [:key  :struct :func :no-idx :seq-idx :par-idx :task])))
