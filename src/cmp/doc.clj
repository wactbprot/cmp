(ns cmp.doc
  ^{:author "wactbprot"
    :doc "Handles the documents in which the 
          produced data is stored in.
          This may be calibration documents but 
          also measurement docs."}
  (:require [cmp.lt-mem :as lt]
            [cmp.st-mem :as st]
            [cmp.utils :as u]
            [clojure.string :as string]
            [taoensso.timbre :as timbre]))

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

(defn path->kw-vec
  "Turns the path into a vector of
  keywords.

  ```clojure
  (path->kw-vec \"a.b.c\")
  ;; [:a :b :c]
  ```"
  [s]
  {:pre (string? s)}
  (into []
        (map
         keyword
         (string/split s (re-pattern "\\.")))))

(defn fit-in
  "Fits m into the struct"
  [struct m]
  (if-let [t (:Type m)]
    (let [is-type? (fn [x]  (= (:Type x) t))
          idx-ok?  (fn [i x] (when (is-type? x) i))]
          (if-let [idx (keep-indexed idx-ok? struct)]
            (prn idx)
            (conj struct m)))))

(defn store-result
  "Stores the result (typically a `Type`, `Value` `Unit` map)
  to the given `doc`ument."  
  [doc m path]
  (let [kw   (path->kw-vec path)]
    (if-let [struct (get-in doc kw)]
      (fit-in struct m)
      (assoc-in doc kw m))))
    
(defn store-results
  "Takes a vector of maps. Calls `store-result`
  on each map."
  [doc res-vec path]
  (reduce 
   (fn [doc m] (store-result doc m path))
   doc res-vec))