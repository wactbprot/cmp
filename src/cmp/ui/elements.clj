(ns cmp.ui.elements
  (:require
   [cmp.config     :as config]
   [cmp.ui.index   :as ui]
   [cmp.api-utils  :as au]
   [clojure.pprint :as pp]
   [cmp.utils :as u]))

(defn content
  [conf req data]
  (prn data))

(defn view [conf req data] (ui/index conf req (content conf req data)))
