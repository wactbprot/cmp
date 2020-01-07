(ns cmp.worker.select
  ^{:author "wactbprot"
    :doc "Worker selects a definition frem the same `mp-id` 
          by evaluating the related conditions."}
  (:require [taoensso.timbre :as timbre]
            [clojure.core.async :as a]
            [clojure.string :as string]
            [cmp.st-mem :as st]
            [cmp.utils :as u]))

(defn get-comp-path
  [mp-id s]
  (let [[l0 l1] (string/split s (re-pattern "\\."))]
    (cond
      (not (nil? l1)) (u/json->map (st/key->val (u/vec->key [mp-id "exchange" "l0"])))
      (not (nil? l0)) (st/key->val (u/vec->key [mp-id "exchange" "l0"]))
      :else (timbre/error "unvalid exchange path structure"))))

(defn cond-match?
  [mp-id k]
  (let [cond-m (u/json->map (st/key->val k))
        a      (get-comp-path mp-id (cond-m :ExchangePath))
        b      (cond-m :Value)
        meth   (cond-m :Methode)]
        
    ))

(defn conds-match?
  [mp-id k]
  (let [mp-id    (u/key->mp-name k)
        exch-ks  (st/pat->keys (u/vec->key))
        cond-ks  (st/pat->keys (u/replace-key-at-level 3 k "cond@*"))]
    (filter cond-match? cond-ks)))

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
  (let [mp-id    (u/key->mp-name state-key)
        def-ks   (st/get-keys-where-val
                  (u/vec->key [mp-id "definitions" "*" "class"])
                  (task :DefinitionClass))]
        (filter conds-match? def-ks)
        (st/set-val! state-key "executed")))
