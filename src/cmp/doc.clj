(ns cmp.doc
  ^{:author "wactbprot"
    :doc "Handles the documents in which the produced data is stored in.
         This may be calibration documents but also measurement docs."}
  (:require [taoensso.timbre :as log]
            [cmp.lt :as lt]
            [cmp.st :as st]
            [cmp.utils :as u])
  (:gen-class))

(defn add
  [p doc-id]
  (let [path (u/get-id-path p doc-id)
        doc (lt/get-doc doc-id)
        info (lt/extr-info doc)]
    (st/set-val! path (u/gen-value info))))

(defn del
  [p doc-id]
  (st/del-key! (u/get-id-path p doc-id)))

(defn get-ids
  [p]
  (let [ks (st/get-keys (u/vec->key [p "id"]))]
    (mapv
     (fn [k] (u/id-key->id k))
     ks)))
