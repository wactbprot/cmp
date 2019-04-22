(ns cmp.core
  (:require [cmp.lt :as lt]
            [cmp.st :as st]
            [cmp.utils :as u]
            [cmp.build :as b]
            [cmp.prep :as p])
  (:gen-class))

(defn main
  "Loads document from long term memory and fetches it to short term memory"
  [id]
    (b/distrib (lt/get-document id)))

(defn clear
  [id]
  "Clears all short term memory for the given id"
  (st/clear (u/extr-main-path id)))

(defn prep
  [id i]
  "Loads the ith container"
  (p/container (u/extr-main-path id) i))