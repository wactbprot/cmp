(ns cmp.core
  (:require [cmp.lt :as lt]
            [cmp.st :as st]
            [cmp.utils :as u]
            [cmp.build :as b]
            [cmp.prep :as p])
  (:gen-class)
  (:use [clojure.repl]))

(defn load-mp
  "Loads document from long term memory and fetches it to short term memory"
  [id]
  (b/store (lt/get-document id)))

(defn clear-mp
  "Clears all short term memory for the given id"
  [id]
  (st/clear (u/extr-main-path id)))

(defn prep-mp-cont
  "Prepairs the ith container"
  [id i]
  (p/container (u/extr-main-path id) i))
