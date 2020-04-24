(ns cmp.worker.select
  ^{:author "wactbprot"
    :doc "Worker selects a definition from the same `mp-id` 
          by evaluating the related conditions."}
  (:require [taoensso.timbre :as timbre]
            [clojure.core.async :as a]
            [clojure.string :as string]
            [cmp.st-mem :as st]
            [cmp.exchange :as exch]
            [cmp.utils :as u]
            [cmp.config :as cfg]))

(def mtp (cfg/min-task-period (cfg/config)))


(defn cond-match?
  "Tests a single condition of the form defined in
  the `definitions` section.
  
  **Example**
  
  Condition maps `m` looks like this:
  
  ```clojure
  (u/json->map (st/key->val \"ref@definitions@0@cond@0\"))
  ;;
  ;; {
  ;;  :ExchangePath \"A.Unit\" ; the value under ref@exchange@A/Unit
  ;;  :Methode \"eq\"          ; should be equal to
  ;;  :Value \"Pa\"            ; Pa
  ;; }
  ```
  "
  [k]
  (let [mp-id (st/key->key-space k)
        m     (st/key->val    k)
        p     (:ExchangePath m)
        a     (str (exch/comp-val mp-id p))
        b     (str (:Value m))]
    (condp = (keyword (:Methode m))
      :eq (= a b)
      :lt (< (read-string a) (read-string b))
      :gt (> (read-string a) (read-string b)))))

(defn conds-match?
  "Gathers all information for the given
  definitions key comparison and checks
  all of the found conditions.
  
  ```clojure
  (conds-match? \"ref@definitions@0@class\")
  ;; false
  (conds-match? \"ref@definitions@1@class\")
  ;; true

  ```
  "
  [k]
  (let [mp-id    (st/key->key-space k)
        no-idx   (st/key->no-idx k)
        k-pat    (u/vec->key [mp-id "definitions" no-idx "cond@*"])
        cond-ks  (st/pat->keys k-pat)
        match-ks (filter cond-match? cond-ks)] 
    (=
     (count cond-ks)
     (count match-ks))))

(defn start-defs
  "Starts the filtered out `definitions` structure and
  sets the state of the calling element to executed if the `ctrl`
  turns to ready (or error if error).

  TODO
  
  ```clojure
  (let [mp-id     (st/key->key-space state-k)
        defs-idx  (st/key->no-idx match-k)
        ctrl-k    (u/vec->key [mp-id \"definitions\" defs-idx \"ctrl\"])])
  ```

  should be replaced by

  ```clojure
  (st/defins-ctrl-path ...)
  ```
  "          
  [match-k state-k]
  (timbre/debug "start definitions struct " match-k)
  (let [mp-id     (st/key->key-space state-k)
        defs-idx  (st/key->no-idx match-k)
        ctrl-k    (st/defins-ctrl-path mp-id defs-idx)
        callback  (fn
                    [p]
                    (cond
                      (= "ready"
                         (st/key->val ctrl-k)) (do
                                                 (st/set-val! state-k "executed")
                                                 (st/de-register! mp-id "definitions" defs-idx "ctrl"))
                      (= "error"
                       (st/key->val ctrl-k)) (st/set-val! state-k "error")))]
    
    (st/register! mp-id "definitions" defs-idx "ctrl" callback)
    (st/set-val! ctrl-k "run")))


(defn select-definition!
  "Selects and runs a `Definition` from the `Definitions`
  section of the current `mp`. 
  
  ```clojure
  (select-definition! {:Action select
                       :TaskName Common-select_definition,
                       :DefinitionClass wait} \"teststate\")
  ```
  If more than one than one of the definitions condition match
  the first is used:

  ```clojure
  (first (filter conds-match? match-ks))
  ;; ref@definitions@1@class
  ```" 
  [task state-k]
  (st/set-val! state-k "working")
  (Thread/sleep mtp)
  (timbre/debug "start with select, already set " state-k " working")
  (let [mp-id     (st/key->key-space state-k)
        def-cls   (task :DefinitionClass)
        def-pat   (u/vec->key [mp-id "definitions" "*" "class"])
        match-ks  (sort
                   (st/filter-keys-where-val def-pat def-cls))
        match-k   (first (filter conds-match? match-ks))]
    (start-defs match-k state-k)))
