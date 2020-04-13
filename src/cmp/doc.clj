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


;;------------------------------
;; extract doc info
;;------------------------------
(defn base-info
  "Returns a map with documents base info."
  [doc]
  {:doc-version (lt/doc->version doc)
   :doc-id (lt/doc->id doc)})

(defn doc-type
  "Returns the type of the document. Assumes the
  type of the document to be the
  first key hierarchy beside `:_id` and `:_rev`."
  [doc m]
  (first
   (filter (fn [kw] (not
              (or (= :_id kw) (= :_rev kw))))
    (keys doc))))

(defmulti doc-info
  "Extracts informations about a document
  depending on the type."
  doc-type)

(defmethod doc-info :Calibration
  [doc m]
  (assoc m
         :doc-type "Calibration"))

(defmethod doc-info :Measurement
  [doc m]
  (assoc m
         :doc-type "Measurement"))

(defmethod doc-info :State
  [doc m]
  (assoc m
         :doc-type "State"))

(defmethod doc-info :default
  [doc m]
  (assoc m
         :doc-type "default"))

;;------------------------------
;; add
;;------------------------------
(defn add
  "Adds a info map to the short term memory."
  [mpd-id id]
  (if-let [doc (lt/id->doc id)]
    (let [k    (st/get-id-path mpd-id id)
          info (doc-info doc (base-info doc))]
      (st/set-val! k info))
    (timbre/error "no doc added")))

;;------------------------------
;; rm
;;------------------------------
(defn rm
  "Removes the info map from the short term memory."
  [mpd-id id]
  (st/del-key! (st/get-id-path mpd-id id)))

;;------------------------------
;; ids
;;------------------------------
(defn ids
  "Returns the list of ids added.
  ```clojure
  (add \"devs\" \"cal-2018-ce3-kk-75003_0002\")
  ;; hiob DEBUG [cmp.lt-mem:14] - try to get 
  ;; document with id: cal-2018-ce3-kk-75003_0002
  ;; OK
  (ids \"devs\")
  ;; (cal-2018-ce3-kk-75003_0002)
  ```"
  [mp-id]
  (map
   (fn [k] (u/key-at-level k 2))
   (st/key->keys
    (st/get-id-prefix mp-id))))
  
;;------------------------------
;; data to doc
;;------------------------------
(defn vector-vals
  "Ensures that the values behind `:Value`,
  `:SdValue` and `:N` are vectors."
  [m]
  (-> m
      (u/vector-if :Value)
      (u/vector-if :SdValue)
      (u/vector-if :N)))

(defn append-and-replace
  "Append `:Value`, `:SdValue` and `:N` if present.
  Relaces `:Type` and `:Unit`."  
  [struct {t :Type v :Value u :Unit n :N s :SdValue}]
  (->
   (-> struct
       (u/replace-if :Type t)
       (u/replace-if :Unit u))
   (u/append-if :Value v)
   (u/append-if :SdValue s)
   (u/append-if :N n)))

(defn fit-in-struct
  "Fits `m` into the given structure `s`. Function
  looks up the `:Type` of `m`. If a structure with
  the same `:Type` exist [[append-and-replace]] is
  called."
  [s m]
  (if-let [t (:Type m)]
    (let [same-type? (fn [x]  (= (:Type x) t))
          idx?       (fn [i x] (when (same-type? x) i))]
          (if-let [idx (first (keep-indexed idx? s))]
            (assoc s idx (append-and-replace (nth s idx) m))
            (conj s (vector-vals m))))))

(defn store-result
  "Stores the result map `m` in the given `doc`ument
  under `p`ath. If `m` contains `:Type` and `:Value` `m`
  is [[fit-in-struct]] and the structure `s` is assumed
  to be a `vector`. Other cases (e.g. merge in `:AuxValues`)
  are straight forward (see [[cmp/test/cmp/doc_test.clj]]
  for details)."
  [doc m p]
  (let [kw-vec (u/path->kw-vec p)]
    (if (and (:Type m) (:Value m))
      (if-let [s (get-in doc kw-vec)]
        (assoc-in doc kw-vec (fit-in-struct s m))
        (assoc-in doc kw-vec [(vector-vals m)]))
      (if-let [s (get-in doc kw-vec)]
        (assoc-in doc kw-vec (merge s m))
        (assoc-in doc kw-vec m)))))

(defn store-results
  "Takes a vector of maps. Calls `store-result`
  on each map."
  [doc res-vec path]
  (reduce 
   (fn [doc m] (store-result doc m path))
   doc res-vec))