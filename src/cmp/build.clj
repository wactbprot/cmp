(ns cmp.build
  ^{:author "wactbprot"
    :doc "Builds up the short term memory with given the mp-definition."}
  (:require [cmp.utils :as u]
            [cmp.task :as tsk]
            [cmp.st :as st]
            [taoensso.timbre :as log])
  (:use [clojure.repl])
  (:gen-class))

(log/set-level! :info)

(defn store-exchange
  "Stores the exchange data."
  [p {exchange :Exchange}]
  (doseq [[k v] exchange]
    (st/set-val! (u/vec->key [p "exchange" (name k)])
            (u/gen-value v))))

(defn store-defin
  "Stores the definition section."
  [p idx defin]
  (doall
   (map-indexed
    (fn [jdx s]
      (doall
       (map-indexed
        (fn [kdx ptsk]
          (st/set-val! (u/get-defin-path p idx jdx kdx) (u/gen-value ptsk)))
        s)))
    defin)))

(defn store-defins
  "Stores the definition_s_ section."
  [p idx cls defin]
  (doall
   (map-indexed
    (fn [jdx s]
      (doall
       (map-indexed
        (fn [kdx ptsk]
          (st/set-val! (u/get-defins-path p idx cls jdx kdx) (u/gen-value ptsk)))
        s)))
    defin)))

(defn store-conds
  "Stores the definitions conditions."
  [p idx cls conds]
  (doall
   (map-indexed
    (fn [jdx c]
          (st/set-val! (u/get-conditions-path p idx cls jdx) (u/gen-value c)))
        conds)))

(defn store-container
  [p idx cont]
  (let [{descr :Description
         title :Title
         ctrl :Ctrl
         elem :Element
         defin :Definition} cont
        ep "container"]           
    (st/set-val! (u/vec->key [p ep idx "title"]) title)
    (st/set-val! (u/vec->key [p ep idx "description"]) descr)
    (st/set-val! (u/vec->key [p ep idx "ctrl"]) (u/gen-value ctrl))
    (st/set-val! (u/vec->key [p ep idx "elem"]) (u/gen-value elem))
    (store-defin p idx defin)))

(defn store-all-container
  "Stores the containers"
  [p {conts :Container}]
  (doall
   (map-indexed
    (fn [idx cont] (store-container p idx cont))
    conts)))

(defn store-definitions
  "Stores a definition given in the definition section
  (second way beside container to provide definitions). This includes
  definitionclass and conditions"
  [p idx ds]
  (let [{cls :DefinitionClass
         descr :ShortDescr
         conds :Condition
         defin :Definition} ds
        ep "definitions"]
    (st/set-val! (u/vec->key [p "definitions" idx "description" cls]) descr)
    (store-conds p idx cls conds)
    (store-defins p idx cls defin)))

(defn store-all-definitions
  "Triggers the storing of the definition section."
  [p {defins :Definitions}]
  (doall
   (map-indexed
    (fn [idx ds] (store-definitions p idx ds))
    defins)))

(defn store-meta
  "Stores the mp meta data."
  [p {standard :Standard name :Name descr :Describtion}]
  (let [ep "meta"]
    (st/set-val! (u/vec->key [p ep "standard"])  standard)
    (st/set-val! (u/vec->key [p ep "name"]) name)
    (st/set-val! (u/vec->key [p ep "description"]) descr)))

(defn store
  "Triggers the storing of meta. exchange etc. to
  the short term memory"
  [{id :_id rev :_rev mp :Mp}]
  (let [p (u/extr-main-path id)]
    (st/clear [p "meta"])
    (store-meta p mp)
    (st/clear [p "exchange"])
    (store-exchange p mp)
    (st/clear [p "container"])
    (store-all-container p mp)
    (st/clear [p "definitions"])
    (store-all-definitions p mp)
    ))
