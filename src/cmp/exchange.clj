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
  (u/get-exch-path
   (u/get-exch-prefix mp-id)
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


(defn from
  "
  {
  :%stateblock1 Vraw_block1
  :%stateblock2 Vraw_block2
  :%stateblock3 Vraw_block3
  :%stateblock4 Vraw_block4
  }"
  [m mp-id]
  (println "----------------------------_")
  (println m)
    (println "----------------------------_")
  )
