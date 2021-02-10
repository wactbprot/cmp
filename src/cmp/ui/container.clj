(ns cmp.ui.container
  (:require
     [cmp.ui.core :as ui]))

(defn view [conf data] (ui/index conf (ui/table conf data)))
