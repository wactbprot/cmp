(ns cmp.exchange
  ^{:author "wactbprot"
    :doc "Handles the access to the exchange interface."}
  (:require [taoensso.timbre :as timbre]
            [clojure.core.async :as a]
            [clojure.string :as string]
            [cmp.st-mem :as st]
            [cmp.utils :as u]))


(defn ->key
  "Returns the base key for the exchange path.

  ```clojure
  (->key  \"foo\" \"bar.baz\")
  ;; \"foo@exchange@bar\"
  (->key \"foo\" \"bar\")
  ;; \"foo@exchange@bar\"
  ```
  "
  [mp-id s]
  {:pre [(not (nil? s))]}
  (u/get-exch-path mp-id
   (first (string/split s (re-pattern "\\.")))))

(defn key->kw
  "Returns the keyword or nil.

  ```clojure
  (key->kw \"foo\" )
  ;; nil
  (key->kw \"foo.bar\" )
  ;; :bar
  ```"  
  [s]
  (if-let [x (second (string/split s (re-pattern "\\.")))] 
    (keyword x)))


(defn from
  "
  Builds a map by replacing the values of the input map.
  The values are gathered from the `exchange` interface
  with the keys: `<mp-id>@exchange@<input-map-value>`.

  ```clojure
  (from \"modbus\" {:%stateblock1 \"Vraw_block1\"
                    :%stateblock2 \"Vraw_block2\"
                    :%stateblock3 \"Vraw_block3\"
                    :%stateblock4 \"Vraw_block4\"})
  ```
  **Todo**
  check for non trivial `<input-map-value>` like
  `{:%aaa \"bbb.ccc\"}`
  "
  [mp-id m]
  (if (or
       (map? m)
       (string? mp-id))
    (u/apply-to-map-values
     (fn [v] (st/key->val (->key mp-id v)))
     m)))
