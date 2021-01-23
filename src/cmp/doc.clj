(ns cmp.doc
  ^{:author "wactbprot"
    :doc "Handles the documents in which the produced data is stored
          in.  This may be calibration documents but also measurement
          docs."}
  (:require [cmp.lt-mem              :as lt]
            [cmp.key-utils           :as ku]
            [cmp.st-mem              :as st]
            [cmp.utils               :as u]
            [vl-data-insert.core     :as insert]
            [clojure.string          :as string]
            [com.brunobonacci.mulog  :as mu]))

(defn doc->version
  "Returns the version of the document as an integer value:"
  [{rev :_rev}]
  (if-let [v (first (string/split rev  #"-"))]
    (Integer/parseInt v)))

(defn doc->id
  "Returns the id of the document."
  [{id :_id}]
  id)

;;------------------------------
;; extract doc info
;;------------------------------
(defn base-info
  "Returns a map with documents base info."
  [doc]
  {:doc-version (doc->version doc)
   :doc-id (doc->id doc)})

(defn doc-type
  "Returns the type of the document. Assumes the type of the document to
  be the first key hierarchy beside `:_id` and `:_rev`."
  [doc m]
  (first
   (filter (fn [kw] (not
              (or (= :_id kw) (= :_rev kw))))
    (keys doc))))

(defmulti doc-info
  "Extracts informations about a document depending on the type."
  doc-type)

(defmethod doc-info :Calibration [doc m] (assoc m :doc-type "Calibration"))
(defmethod doc-info :Measurement [doc m] (assoc m :doc-type "Measurement"))
(defmethod doc-info :State       [doc m] (assoc m :doc-type "State"))
(defmethod doc-info :default     [doc m] (assoc m :doc-type "default"))

;;------------------------------
;; add
;;------------------------------
(defn add
  "Adds a info map to the short term memory."
  [mpd-id id]
  (if-let [doc (lt/id->doc id)]
    (let [k    (ku/id-key mpd-id id)
          info (doc-info doc (base-info doc))]
      (st/set-val! k info))
    (mu/log ::add :error "no info map added" :doc-id id)))

;;------------------------------
;; rm
;;------------------------------
(defn rm
  "Removes the info map from the short term memory."
  [mpd-id id]
  (mu/log ::add :message "will rm doc info from st-mem" :doc-id id)
  (st/del-key! (ku/id-key mpd-id id)))

;;------------------------------
;; ids
;;------------------------------
(defn ids
  "Returns the list of ids added.

  Example:
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
    (ku/id-prefix mp-id))))

;;------------------------------
;; store with doc-lock
;;------------------------------
(def doc-lock (Object.))
(defn store!
  "Stores the `results` vector under the `doc-path` of every document
  loaded at the given `mp-id`. Checks if the version of each document
  is `+1`.  Returns `{:ok true}` or `{:error <problem>}`.

  Example:
  ```clojure
  (def results [{:Type \"cmp-test\" :Unit \"Pa\" :Value 1}
               {:Type \"cmp-test2\" :Unit \"Pa\" :Value 2}])

  (def doc-path  \"Calibration.Measurement.Values.Pressure\")  

  (store! \"ref\" results doc-path)
  ```"  
  [mp-id results doc-path]
  (if (and (string? mp-id)
           (vector? results)
           (string? doc-path))
      (let [ids (ids mp-id)]
        (if (empty? ids)
          {:ok true :warn "no documents loaded"}
          (let [res (map
                     (fn [id]
                       (locking doc-lock
                         (mu/log ::store! :message "lock doc" :doc-id id)
                         (let [in-doc  (lt/id->doc id)
                               doc     (insert/store-results in-doc results doc-path)
                               out-doc (lt/put-doc doc)]
                           (mu/log ::store! :message "release lock" :doc-id id))))
                     ids)]
            (if-let [n-err (:error (frequencies res))]
              {:error "got " n-err " during attempt to store results"}
              {:ok true}))))
    {:ok true :warn "no doc-path or no mp-id or no results"}))
