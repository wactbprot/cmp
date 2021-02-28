(ns cmp.worker.select
  ^{:author "wactbprot"
    :doc "Worker selects a definition from the same `mp-id` 
          by evaluating the related conditions."}
  (:require [cmp.config              :as cfg]
            [cmp.exchange            :as exch]
            [com.brunobonacci.mulog  :as mu]
            [cmp.st-mem              :as st]
            [cmp.st-utils            :as stu]
            [clojure.string          :as string]
            [cmp.utils               :as u]))

(defn cond-match?
  "Tests a single condition of the form defined in
  the `definitions` section.

  Example:
  ```clojure
  (cond-match? 10 :gt 1)
  ;; true
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
  (when (conds-match? v) (first v)))

(defn gen-callback
  [{mp-id :mp-id  no-idx :no-idx state-key :StateKey}]
  (let [ctrl-key (stu/defins-ctrl-key mp-id no-idx)]
    (fn [msg]
      (condp = (keyword (st/key->val ctrl-key))
        :run   (mu/log ::start-defins! :message "run callback for" :key ctrl-key)
        :ready (do
                 (st/set-state! state-key :executed "ready callback") 
                 (st/de-register! mp-id "definitions" no-idx "ctrl" "b"))
        :error (st/set-state! state-key :error "error callback for")))))
  
(defn start-defins!
  "Starts the matching `definitions` structure. `register`s
  a level b callback. Sets the state of the calling element to `executed`
  if the `ctrl`  turns to ready (or error if error)."          
  [{mp-id :mp-id  no-idx :no-idx state-key :StateKey :as task}]
  (let [ctrl-key (stu/defins-ctrl-key mp-id no-idx)]
    (st/register! mp-id "definitions" no-idx "ctrl" (gen-callback task) "b")
    (st/set-state! ctrl-key :run)))

(defn cond-key->cond-map
  "Builds a `cond`ition`-map` belonging to the key `k`. Replaces the
  compare value fetched from the exchange interface by means of the
  `exch/read!`-function.

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
  ``` "
  [k]
  (let [key-map   (stu/key->info-map k)
        val-map   (st/key->val k)
        left-val  (exch/read! (:mp-id key-map) (:ExchangePath val-map))
        meth      (:Methode val-map)
        right-val (:Value val-map)]
    (assoc key-map :cond-match (cond-match? left-val meth right-val))))

(defn class-key->cond-keys
  "Turns a `class-key` into `cond-keys`."
  [k]
  (when k
    (let [m (stu/key->info-map k)]
      (st/key->keys (stu/defins-cond-key (:mp-id m) (:no-idx m))))))

(defn class-keys
  "Returns the keys where the class is `cls`."
  [mp-id cls]
  (let [pat (stu/defins-class-key mp-id "*")]
    (st/filter-keys-where-val pat cls)))

(defn select-definition!
  "Selects and runs a `Definition` from the `Definitions` section of the
  current `mp`. Builds a `cond`ition`-map` (analog to the `state-map`)
  in order to avoid the spreading of side effects and easy testing.
  
  Example:
  ```clojure
  (ns cmp.worker.select)
  (select-definition! {:MpName ref
                       :Action select
                       :TaskName Common-select_definition,
                       :DefinitionClass wait} )
  ;; match map:
  {:mp-id ref,
  :struct definitions
  :no-idx 1,
  :func cond
  :seq-idx 1,
  :par-idx nil,
  :cond-match true}
  ```
  If more than one than one of the definitions condition match
  the first is used:

  ```clojure
  (first (filter conds-match? match-ks))
  ;; ref@definitions@1@class
  ```" 
  [{mp-id :MpName cls :DefinitionClass state-key :StateKey}]
  (st/set-state! state-key :working)
  (let [cond-keys (mapv class-key->cond-keys (class-keys mp-id cls))
        cond-vec  (mapv (fn [ks]
                          (mapv cond-key->cond-map ks))
                        cond-keys)]
    (if-let [match-map (first (remove nil? (map filter-match cond-vec)))]
      (start-defins! (assoc match-map :StateKey state-key))
      (st/set-state! state-key :error "no matching definition"))))
