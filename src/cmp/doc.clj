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
   (filter (fn [kw] (not
              (or (= :_id kw) (= :_rev kw))))
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
  {:pre [(string? s)]}
  (into []
        (map
         keyword
         (string/split s (re-pattern "\\.")))))

(defn ensure-vector-val
  "Ensures that `v` is a vector.

  ```clojure
  (ensure-vector-val nil) ;!
  ;; nil
  (ensure-vector-val 1)
  ;; [1]
  (ensure-vector-val [1])
  ;; [1]
  ```"
  [v]
  (if-let [x v]
    (if (vector? x)
      x
      [x])))

(defn vector-if
  "Makes the value `v` behind the keyword `kw`
  a vector if `v` is not nil."
  [m kw]
  (if (and (map? m) (keyword? kw))
    (if-let [v (kw m)]
      (assoc m kw (ensure-vector-val v))
      m)))

(defn ensure-vector-vals
  "Ensures that the values behind `:Value`,
  `:SdValue` and `:N` are vectors."
  [m]
  (-> m
      (vector-if :Value)
      (vector-if :SdValue)
      (vector-if :N)))

(defn replace-if
  "Replaces `v`alue of `k`ey in struct
  if `v`is not `nil`.

  ```clojure
  (replace-if {:Type \"a\"} :Type \"b\")
  ;; {:Type \"b\"}
  ```
  "
  [m k v]
  (if (and (some? v) (keyword? k))
    (assoc m k v)
    m))

(defn append-if
  "Appends `v` to the value of `k`.
  If `k` does not exist in `m`, `k [v]` is assoced.
  If `k` does exist in `m`, `v` is conjed.
  
  ```clojure
  (append-if {:Value [1 2 3]} :Value 4)
  ;; {:Value [1 2 3 4]}"
  [m k v]
  (if (and (some? v) (keyword? k))
    (let [new-v (ensure-vector-val v)]
      (if-let [old-v (k m)]
        (assoc m k (into [] (concat old-v new-v)))
        (assoc m k new-v)))
    m))

(defn append-and-replace
  "Append `:Value`, `:SdValue` and `:N` if present.
  Relaces `:Type` and `:Unit`."  
  [struct {t :Type v :Value u :Unit n :N s :SdValue}]
  (->
   (-> struct
       (replace-if :Type t)
       (replace-if :Unit u))
   (append-if :Value v)
   (append-if :SdValue s)
   (append-if :N n)))

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
            (conj s (ensure-vector-vals m))))))

(defn store-result
  "Stores the result (typically a `:Type`,
  `:Value` `:Unit` map) to the given `doc`ument
  under `p`ath."  
  [doc m p]
  (let [kw-vec (path->kw-vec p)]
    (if (and (:Type m) (:Value m))
      (if-let [s (get-in doc kw-vec)]
        (assoc-in doc kw-vec (fit-in-struct s m))
        (assoc-in doc kw-vec [(ensure-vector-vals m)]))
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