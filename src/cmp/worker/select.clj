(ns cmp.worker.select
  ^{:author "wactbprot"
    :doc "Worker selects a definition from the same `mp-id` 
          by evaluating the related conditions."}
  (:require [taoensso.timbre :as timbre]
            [clojure.core.async :as a]
            [clojure.string :as string]
            [cmp.st-mem :as st]
            [cmp.utils :as u]))

(defn get-exch-path
  "Returns the base key for the exchange path.
  ```clojure
  (get-exch-path  \"foo\" \"bar.baz\")
  ;; \"foo@exchange@bar\"
  (get-exch-path \"foo\" \"bar\")
  ;; \"foo@exchange@bar\"
  ```
  "
  [mp-id s]
  (u/vec->key
   [mp-id
    "exchange"
    (first (string/split s (re-pattern "\\.")))]))

(defn get-exch-kw
  "Returns the keyword or nil.
    ```clojure
  (get-exch-kw \"foo\" )
  ;; nil
  (get-exch-kw \"foo.bar\" )
  ;; :bar
  ```" 
  [s]
  (if-let [x (second (string/split s (re-pattern "\\.")))] 
    (keyword x)))

(defn cond-match?
  [a b meth]
  ;; todo
  )

(defmulti get-nested-val
  "Returns the value belonging to key `k`.
  If the *keyword* `kw` is not `nil` it is
  used to extract the related value.

  ```clojure
  (get-nested-val \"se3-calib@definitions@26@cond@0\" :Value)
  ;; \"f_l\"
  ```"
  (fn [k kw] (nil? kw)))

(defmethod get-nested-val true
  [k kw]
  (st/key->val k))

(defmethod get-nested-val false
  [k kw]
  ((u/json->map (st/key->val k)) kw))


(defn conds-match?
  "Gathers all information for the given
  definitions key"
  [k]
  (let [mp-id    (u/key->mp-name k)
        cond-ks  (st/pat->keys (u/replace-key-at-level 3 k "cond@*"))]
    (filter
     (fn [k]
       (let [cond-m    (u/json->map (st/key->val k))
             a         (cond-m :Value)
             meth      (cond-m :Methode)
             exch-p    (cond-m :ExchangePath)
             exch-k    (get-exch-path mp-id exch-p)
             exch-kw   (get-exch-kw  exch-p)
             b         (get-nested-val exch-p exch-kw)]
         
         (cond-match? a b meth)))
     cond-ks)))

(defn select-definition!
  "Selects and runs a `Definition` from the `Definitions`
  section of the current `mp`. 
  
  ```clojure
  (select-definition! {:Action select
                       :Break no,
                       :TaskName Common-select_definition,
                       :DefinitionClass wait
                       :Replace {%definitionclass wait}} \"testpath\"
  ```"
  [task state-key]
  (st/set-val! state-key "working")
  (let [mp-id     (u/key->mp-name state-key)
        def-cls   (task :DefinitionClass)
        def-ks    (u/vec->key [mp-id "definitions" "*" "class"])
        match-ks  (st/get-keys-where-val def-ks def-cls)]
    (filter conds-match? match-ks)
    (st/set-val! state-key "executed")))
