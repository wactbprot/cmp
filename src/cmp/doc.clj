(ns cmp.doc
  ^{:author "wactbprot"
    :doc "Handles the documents the produced data is stored in.
         This may be calibration documents but also measurement docs."}
  (:require [taoensso.timbre :as log]
            [cmp.lt :as lt]
            [cmp.st :as st]
            [cmp.utils :as u])
  (:gen-class))

(defn extr-doc-type [doc]
  (first
   (filter
    (fn [kw]  (not
               (or
                (= :_id kw)
                (= :_rev kw))))
    (keys doc))))


(defmulti extr-info
  extr-doc-type)

(defmethod extr-info :Calibration
  [doc])

(defmethod extr-info :Measurement
  [doc])

(defmethod extr-info :State
  [doc])


(defn add
  [mp-id doc-id]
  (let [doc (lt/get-doc doc-id)
        info (extr-info doc)]
    info
    ))