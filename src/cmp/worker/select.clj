(ns cmp.worker.select
  ^{:author "wactbprot"
    :doc "select a definition worker."}
  (:require [taoensso.timbre :as timbre]
            [clojure.core.async :as a]
            [clojure.string :as string]
            [cmp.st-mem :as st]
            [cmp.utils :as u]))

(defn cond-match?
  [k]
  (let [cond-m (u/json->map (st/key->val k))
        [exch-p exch-t] (string/split (cond-m :ExchangePath) (re-pattern "\\."))
        meth   (cond-m :Methode)
        val    (cond-m :Value)]
    ))

(defn conds-match?
  [k]
  (let [mp-id    (u/key->mp-name k)
        exch-ks  (st/pat->keys (u/vec->key [mp-id "exchange" "*"]))
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