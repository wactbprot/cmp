(ns cmp.ui.listener
  (:require
     [cmp.ui.core :as ui]))

(defn view [conf data] (ui/index conf (ui/table conf data)))
