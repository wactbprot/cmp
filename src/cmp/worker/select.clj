(ns cmp.worker.select
  ^{:author "wactbprot"
    :doc "Worker selects a definition from the same `mp-id` 
          by evaluating the related conditions.
         "}
  (:require [taoensso.timbre :as timbre]
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
  (let [mp-id (st/key->mp-id k)
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
  (let [mp-id    (st/key->mp-id k)
        no-idx   (st/key->no-idx k)
        k-pat    (u/vec->key [mp-id "definitions" no-idx "cond@*"])
        cond-ks  (st/pat->keys k-pat)
        match-ks (filter cond-match? cond-ks)] 
    (=
     (count cond-ks)
     (count match-ks))))

(defn start-defins!
  "Starts the matching `definitions` structure and
  sets the state of the calling element to `executed`
  if the `ctrl`  turns to ready (or error if error).

  "          
  [match-k state-k]
  (timbre/debug "start definitions struct " match-k)
  (let [struct "definitions"
        func   "ctrl"
        mp-id  (st/key->mp-id state-k)
        no-idx (st/key->no-idx match-k)
        ctrl-k (st/defins-ctrl-path mp-id no-idx)
        cb!    (fn [msg]
                 (condp = (keyword (st/key->val ctrl-k))
                   :run   (timbre/debug "start-defs! run callback for" ctrl-k)
                   :ready (do
                            (timbre/debug "start-defs! ready callback for" ctrl-k)
                            (st/set-val! state-k "executed"))
                   :error (do
                            (timbre/error "start-defs! error callback for" ctrl-k)
                            (st/set-val! state-k "error"))))]
    (st/set-val! ctrl-k "run")
    (st/register! mp-id struct no-idx func cb!)))

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
  (let [mp-id     (st/key->mp-id state-k)
        def-cls   (task :DefinitionClass)
        def-pat   (u/vec->key [mp-id "definitions" "*" "class"])
        match-ks  (sort
                   (st/filter-keys-where-val def-pat def-cls))]
    (if-let [match-k (first (filter conds-match? match-ks))]
      (start-defins! match-k state-k)
      (timbre/error "nothing match"))))
