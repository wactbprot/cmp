(ns cmp.lt
  (:require [com.ashafa.clutch :as couch])
  (:gen-class))

(def conn "http://localhost:5984/vl_db")

(defn get-document [id]
  (couch/get-document conn id))