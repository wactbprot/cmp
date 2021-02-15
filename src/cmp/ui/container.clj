(ns cmp.ui.container
  (:require
     [cmp.ui.core :as ui]))

(defn view
  [conf data]
  (ui/index conf (ui/table conf data [:key :mp-id :struct :func :no-idx :value])))
