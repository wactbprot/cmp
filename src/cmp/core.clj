(ns cmp.core
  (:require [cmp.lt :as lt]
            [cmp.st :as st]
            [cmp.utils :as u]
            [cmp.doc :as d]
            [cmp.build :as b]
            [cmp.prep :as p])
  (:gen-class)
  (:use [clojure.repl]))

(defn load-mp
  "Loads document from long term memory and fetches it to short term memory"
  [id]
  (b/store (lt/get-doc id)))

(defn clear-mp
  "Clears all short term memory for the given id"
  [id]
  (st/clear (u/extr-main-path id)))

(defn prep-cont
  "Prepairs the ith container"
  [id i]
  (p/container (u/extr-main-path id) i))

(defn add-doc
  "Adds a doc to the api to store the resuls in."
  [mp-id doc-id]
  (d/add mp-id doc))

(defn rm-doc
  "Removes a doc to the api to store the resuls in."
  [mp-id doc-id]
  (d/rm mp-id doc))