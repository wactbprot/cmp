(ns cmp.ui.container
  (:require
     [cmp.ui.core :as ui]))

(defn view-ctrl
  [conf data mp]
  (ui/index conf
            (ui/table conf data [:key  :struct :func :no-idx :title :value :run :stop :mon]) mp))

(defn view-state
  [conf data mp]
  (ui/index conf
            (into [:div]
                  [[:h3 {:class "title"} (:title (first data))]
                   (ui/table conf data [:key  :struct :func :no-idx :value :ready :working :executed])]) mp))

(defn view
  [conf data mp]
  (ui/index conf (ui/table conf data [:key  :struct :func :no-idx :value]) mp))
