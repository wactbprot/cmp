(ns cmp.doc
  ^{:author "wactbprot"
    :doc "Handles the documents in which the 
          produced data is stored in.
          This may be calibration documents but 
          also measurement docs."}
  (:require [taoensso.timbre :as log]
            [cmp.lt-mem :as lt]
            [cmp.st-mem :as st]
            [cmp.utils :as u]))

(defn base-info
  [doc]
  {:doc-version (lt/doc->version doc)
   :doc-id (lt/doc->id doc)})

(defn extr-doc-type
  "Extracts the document type. Assumes the
  type of the document to be the
  first key hierarchy beside `:_id` and `:_rev`."
  [doc m]
  (first
   (filter
    (fn [kw] (not
              (or
               (= :_id kw)
               (= :_rev kw))))
    (keys doc))))

(defmulti extr-info
  extr-doc-type)

(defmethod extr-info :Calibration
  [doc m]
  (assoc m
         :doc-type "Calibration"))

(defmethod extr-info :Measurement
  [doc m]
  (assoc m
         :doc-type "Measurement"))

(defmethod extr-info :State
  [doc m]
  (assoc m
         :doc-type "State"))

(defmethod extr-info :default
  [doc m]
  (assoc m
         :doc-type "default"))

(defn add
  [p id]
  (let [path (st/get-id-path p id)
        doc  (lt/id->doc id)
        info (extr-info doc (base-info doc))]
    (st/set-val! path info)))

(defn del
  [p id]
  (st/del-key! (st/get-id-path p id)))

(defn get-ids
  [p]
  (let [ks (st/key->keys (u/vec->key [p "id"]))]
    (mapv
     (fn [k] (u/id-key->id k))
     ks)))
