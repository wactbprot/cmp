(ns cmp.ui.listener
  (:require
     [cmp.ui.core :as ui]))

(defn view [conf data]
  (ui/index conf (ui/table conf data [:key :mp-id :struct :level :no-idx ])))
