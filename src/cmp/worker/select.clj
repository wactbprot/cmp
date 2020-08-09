(ns cmp.worker.select
  ^{:author "wactbprot"
    :doc "Worker selects a definition from the same `mp-id` 
          by evaluating the related conditions.
         "}
  (:require [taoensso.timbre :as log]
            [clojure.string :as string]
            [cmp.st-mem :as st]
            [cmp.exchange :as exch]
            [cmp.utils :as u]
            [cmp.config :as cfg]))

(def mtp (cfg/min-task-period (cfg/config)))

(defn cond-match?
  "Tests a single condition of the form defined in
  the `definitions` section.

  Example:
  ```clojure
  ```
  "
  [l m r]
  (condp = (keyword m)
      :eq (= l r)
      :lt (< (read-string (str l)) (read-string (str r)))
      :gt (> (read-string (str l)) (read-string (str r)))))

(defn conds-match?
  "Checks if `:cond-match` in  every map
  in the `cond`ition-`vec`tor `v` is true."
  [v]
  (every? true? (map :cond-match v)))

(defn filter-match
  "Checks if `:cond-match` in  every map
  in the `cond`ition-`vec`tor `v` is true."
  [v]
  (when (conds-match? v)
    (first v)))

(defn start-defins!
  "Starts the matching `definitions` structure. `register`s
  a level b callback. Sets the state of the calling element to `executed`
  if the `ctrl`  turns to ready (or error if error).

  "          
  [match-k state-k]
  (log/debug "start definitions struct " match-k)
  (let [struct   "definitions"
        func     "ctrl"
        mp-id    (st/key->mp-id state-k)
        no-idx   (st/key->no-idx match-k)
        ctrl-k   (st/defins-ctrl-path mp-id no-idx)
        level    "b"
        callback  (fn [msg]
                    (condp = (keyword (st/key->val ctrl-k))
                      :run   (log/debug "run callback for" ctrl-k)
                      :ready (do
                               (log/debug "ready callback for" ctrl-k)
                               (when state-k
                                 (Thread/sleep mtp)
                                 (st/set-val! state-k "executed")) 
                               (st/de-register! mp-id struct no-idx func level))
                      :error (do
                               (log/error "error callback for" ctrl-k)
                               (when state-k
                                 (Thread/sleep mtp)
                                 (st/set-val! state-k "error")))))]
    (st/register! mp-id struct no-idx func callback level)
    (st/set-val! ctrl-k "run")))

(defn cond-key->cond-map
  "Builds a `cond`ition`-map` belonging to the
  key  `k`. Replaces the compare value fetched
  from the exchange interface by means of the
  `exch/comp-val!`-function.

  Example:
  ```clojure
  (cond-key->cond-map \"ref@definitions@1@cond@0\")
  ;; {:mp-name \"ref\",
  ;;  :struct \"definitions\",
  ;;  :no-idx 1,
  ;;  :no-jdy 0,
  ;;  :comp-value \"Pa\",
  ;;  :meth \"eq\",
  ;;  :exch-value \"Pa\"}

  ;; where:

  (st/key->val \"ref@definitions@1@cond@0\")
  ;;{:ExchangePath \"A.Unit\", :Methode \"eq\", :Value \"Pa\"}

  ;; and:

  (st/key->val \"ref@exchange@A\")
  ;; {:Unit \"Pa\", :Value 100}
  ```
  "
  [k]
  (let [key-map     (st/key->key-map k)
        val-map     (st/key->val k)
        left-val    (exch/comp-val! (:mp-id key-map) (:ExchangePath val-map))
        meth        (:Methode val-map)
        right-val   (:Value val-map)]
    (assoc
     key-map :cond-match (cond-match? left-val meth right-val))))

(defn class-key->cond-keys
  [k]
  (when k
    (let [mp-id   (st/key->mp-id k)
          struct  (st/key->struct k)
          no-idx  (st/key->no-idx k)
          cond-k  (st/defins-cond-path mp-id no-idx)]
      (st/key->keys cond-k))))

(defn class->class-keys
  "Returns the keys where the class is `cls`."
  [mp-id cls]
  (let [pat (st/defins-class-path mp-id "*")]
    (st/filter-keys-where-val pat cls)))

(defn select-definition!
  "Selects and runs a `Definition` from the `Definitions`
  section of the current `mp`. Builds a `cond`ition`-map`
  (analog to the `state-map`) in order to avoid the
  spreading of side effects and easy testing.

  Example:
  ```clojure
  (ns cmp.worker.select)
  (select-definition! {:MpName ref
                       :Action select
                       :TaskName Common-select_definition,
                       :DefinitionClass wait} )
  ```
  If more than one than one of the definitions condition match
  the first is used:

  ```clojure
  (first (filter conds-match? match-ks))
  ;; ref@definitions@1@class
  ```" 
  [{mp-id :MpName cls :DefinitionClass state-key :StateKey}]
  (st/set-val! state-key "working")
  (let [cond-keys (mapv class-key->cond-keys (class->class-keys mp-id cls))
        cond-vec  (mapv (fn [ks] (mapv cond-key->cond-map ks)) cond-keys)]
    (first (remove nil? (map filter-match cond-vec)))))
