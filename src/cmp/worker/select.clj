(ns cmp.worker.select
  ^{:author "wactbprot"
    :doc "select a definition worker."}
  (:require [taoensso.timbre :as timbre]
            [clojure.core.async :as a]
            [cmp.st-mem :as st]
            [cmp.utils :as u]))

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
  (let [mp-id (u/key->mp-name (task :StructKey))
        def-class (task :DefinitionClass)
        def-ks (st/pat->keys (u/vec->key [mp-id "definitions" "*" "class"]))]
    
    (st/set-val! state-key "executed")))
