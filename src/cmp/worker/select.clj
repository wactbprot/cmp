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
  {:pre [(not (nil? s))]}
  (u/vec->key [mp-id "exchange" (first (string/split s (re-pattern "\\.")))]))

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

  (defmulti get-comp-val
  "Returns the *compare value* belonging to key `k`.
  If the *keyword* `kw` is not `nil` it is
  used to extract the related value.

  ```clojure
  (get-comp-val \"ref@definitions@0@cond@0\" :Value)
  ;; \"Pa\"
  ```"
  (fn [k kw] (nil? kw)))

(defmethod get-comp-val true
  [k kw]
  (st/key->val k))

(defmethod get-comp-val false
  [k kw]
  (kw (st/key->val k)))

(defn cond-match?
  "Tests a single condotion of the form defined in
  the `definitions` section.
  
  ```clojure
  ;; one condition looks like this:
  (u/json->map (st/key->val \"ref@definitions@0@cond@0\"))
  {:ExchangePath \"A.Unit\", :Methode \"eq\", :Value \"Pa\"}
  ```
  "
  [k]
  (let [mp-id     (u/key->mp-name k)
        cond-m    (st/key->val k)
        b         (str (cond-m :Value))
        meth      (cond-m :Methode)
        exch-p    (cond-m :ExchangePath)
        exch-k    (get-exch-path mp-id exch-p)
        exch-kw   (get-exch-kw  exch-p)
        a         (str (get-comp-val exch-k exch-kw))]
    (cond
      (= meth "eq") (= a b)
      (= meth "lt") (< (read-string a) (read-string b))
      (= meth "gt") (> (read-string a) (read-string b)))))

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
  (let [mp-id    (u/key->mp-name k)
        no-idx   (u/key->no-idx k)
        k-pat    (u/vec->key [mp-id "definitions" no-idx "cond@*"])
        cond-ks  (st/pat->keys k-pat)
        match-ks (filter cond-match? cond-ks)] 
    (=
     (count cond-ks)
     (count match-ks))))

(defn start-defs
  "Starts the filtered out `definitions` structure and
  sets the state of the calling element to executed if the `ctrl`
  turns to ready (or error if error)."          
  [match-k state-k]
  (timbre/debug "start definitions struct " match-k)
  (let [mp-id     (u/key->mp-name state-k)
        defs-idx  (u/key->no-idx match-k)
        ctrl-k    (u/vec->key [mp-id "definitions" defs-idx "ctrl"])
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
  (timbre/debug "start with select, already set " state-k " working")
  (let [mp-id     (u/key->mp-name state-k)
        def-cls   (task :DefinitionClass)
        def-pat   (u/vec->key [mp-id "definitions" "*" "class"])
        match-ks  (sort
                   (st/filter-keys-where-val def-pat def-cls))
        match-k   (first (filter conds-match? match-ks))]
    (start-defs match-k state-k)))

