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
  (st/get-exch-path mp-id
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

(defn key->val
  "Returns the value from the exchange interface.
  Respects the case where the given  `s` is non trivial."  
  [mp-id s]
  (let [kw  (key->kw s)
        k   (->key mp-id s)
        val (st/key->val k)]
    (if kw
      (kw val)
      val)))

(defn comp-val
  "Returns the *compare value* belonging to a `mp-id`
  and an ExchangePath `k`. Gets the  *keyword* `kw`
  from `k` if `k` looks like this: `aaa.bbb`. If `kw`
  is not `nil` it is used to extract the related value.

  ```clojure
  (comp-val \"ref\" \"A.Unit\")
  ;; \"Pa\"
  ;; or:
  (comp-val \"modbus\" \"Vraw_block1\")
  ;; [1 0 1 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 1 0]
  ```"
  [mp-id p]
  (let [k (->key mp-id p)]
    (if-let [kw  (key->kw p)]
      (kw (st/key->val k))
      (st/key->val k))))
  
(defn from!
  "Builds a map by replacing the values of the input map.
  The replacements are gathered from the `exchange` interface
  with the keys: `<mp-id>@exchange@<input-map-value>`

  Example key: `modbus@exchange@Vraw_block1`
  Example value: `[
                  1,0,1,0,
                  0,0,0,0,
                  0,0,0,0,
                  1,0,0,0,
                  0,0,0,0,
                  0,0,1,0
                  ]`
  Return: `{:%stateblock1 [
                  1,0,1,0,
                  0,0,0,0,
                  0,0,0,0,
                  1,0,0,0,
                  0,0,0,0,
                  0,0,1,0
                  ]}` 
  ```clojure
  (from \"modbus\" {
                    :%stateblock1 \"Vraw_block1\"
                    :%stateblock2 \"Vraw_block2\"
                    :%stateblock3 \"Vraw_block3\"
                    :%stateblock4 \"Vraw_block4\"
                    })
  ```
  
  **Todo**

  Check for non trivial `<input-map-value>` like
  `{:%aaa \"bbb.ccc\"}`
  "
  [mp-id m]
  (if (and (string? mp-id) (map? m))
    (u/apply-to-map-values
     (fn [v] (key->val mp-id v))
     m)))

(defn to!
  "Writes `m` to the exchange interface.
  The first level keys of `m` are used for the
  access path.
  ```clojure
  {:A 1}
  ```
  will be stored under `<mp-id>@exchange@A`."
  [mp-id m]
  (if  (and (string? mp-id) (map? m))
      (doseq [[k v] m]
        (st/set-val! (st/get-exch-path mp-id (name k)) v))))
