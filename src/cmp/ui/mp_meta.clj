(ns cmp.ui.mp-meta
  (:require
     [cmp.ui.core :as ui]))

(defn view [conf data] (ui/index conf (ui/table conf data)))
