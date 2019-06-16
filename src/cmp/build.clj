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
    (st/set-val! (u/get-exch-path p (name k)) (u/gen-value v))))

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

(defn store-container
  [p idx {descr :Description
          title :Title
          ctrl :Ctrl
          elem :Element
          defin :Definition}]           
  (st/set-val! (u/get-cont-title-path p idx) title)
  (st/set-val! (u/get-cont-descr-path p idx) descr)
  (st/set-val! (u/get-cont-ctrl-path p idx) (u/gen-value ctrl))
  (st/set-val! (u/get-cont-elem-path p idx) (u/gen-value elem))
  (store-defin p idx defin))

(defn store-all-container
  "Stores the containers"
  [p {conts :Container}]
  (doall
   (map-indexed
    (fn [idx cont] (store-container p idx cont))
    conts)))

(defn store-defins
  "Stores the definitions section."
  [p cls idx defin]
  (doall
   (map-indexed
    (fn [jdx s]
      (doall
       (map-indexed
        (fn [kdx ptsk]
          (st/set-val! (u/get-defins-defin-path p cls idx jdx kdx) (u/gen-value ptsk)))
        s)))
    defin)))

(defn store-conds
  "Stores the definitions conditions."
  [p idx cls conds]
  (doall
   (map-indexed
    (fn [jdx c]
      (st/set-val! (u/get-defins-cond-path p cls idx jdx) (u/gen-value c)))
        conds)))

(defn store-definitions
  "Stores a definition given in the definition section
  (second way beside container to provide definitions). This includes
  definitionclass and conditions"
  [p idx ds]
  (let [{cls :DefinitionClass
         descr :ShortDescr
         conds :Condition
         defin :Definition} ds]
    (st/set-val! (u/get-defins-descr-path p cls idx) descr)
    (store-conds p cls idx conds)
    (store-defins p cls idx defin)))

(defn store-all-definitions
  "Triggers the storing of the definition section."
  [p {defins :Definitions}]
  (doall
   (map-indexed
    (fn [idx ds] (store-definitions p idx ds))
    defins)))

(defn store-meta
  "Stores the mp meta data."
  [p {standard :Standard
      name :Name
      descr :Describtion}]
  (st/set-val! (u/get-meta-std-path p)  standard)
  (st/set-val! (u/get-meta-name-path p) name)
  (st/set-val! (u/get-meta-descr-path p) descr))

(defn store
  "Triggers the storing of meta. exchange etc. to
  the short term memory. Clears up the field before"
  [{id :_id
    rev :_rev
    mp :Mp}]
  (let [p (u/extr-main-path id)]
    (st/clear (u/get-meta-prefix p))
    (store-meta p mp)
    (st/clear (u/get-exch-prefix p))
    (store-exchange p mp)
    (st/clear (u/get-cont-prefix p))
    (store-all-container p mp)
    (st/clear (u/get-defins-prefix p))
    (store-all-definitions p mp)))
