(ns cmp.build
  ^{:author "wactbprot"
    :doc "Builds up the short term memory with given the mp-definition."}
  (:require [cmp.utils :as u]
            [cmp.task :as tsk]
            [cmp.st :as st]
            [taoensso.timbre :as log])
  (:use [clojure.repl])
  (:gen-class))

;;------------------------------
;; exchange
;;------------------------------
(defn store-exchange
  "Stores the exchange data."
  [p {exchange :Exchange}]
  (doseq [[k v] exchange]
    (st/set-val! (u/get-exch-path p (name k)) (u/gen-value v))))

;;------------------------------
;; container
;;------------------------------
(defn store-defin
  "Stores the definition section."
  [p idx defin]
  (doall
   (map-indexed
    (fn [jdx s]
      (doall
       (map-indexed
        (fn [kdx ptsk]
          (st/set-val! (u/get-cont-defin-path p idx jdx kdx) (u/gen-value ptsk))
          (st/set-val! (u/get-cont-state-path p idx jdx kdx) "build"))
        s)))
    defin)))

(defn store-container
  "Stores a single container"
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
  "Triggers the storing of the singel containers"
  [p {conts :Container}]
  (doall
   (map-indexed
    (fn [idx cont] (store-container p idx cont))
    conts)))

;;------------------------------
;; definitions
;;------------------------------
(defn store-defins
  "Stores the definitions section."
  [p idx defin]
  (doall
   (map-indexed
    (fn [jdx s]
      (doall
       (map-indexed
        (fn [kdx ptsk]
          (st/set-val! (u/get-defins-defin-path p idx jdx kdx) (u/gen-value ptsk))
          (st/set-val! (u/get-defins-state-path p idx jdx kdx) "build"))
        s)))
    defin)))

(defn store-conds
  "Stores the definitions conditions."
  [p idx conds]
  (doall
   (map-indexed
    (fn [jdx c]
      (st/set-val! (u/get-defins-cond-path p idx jdx) (u/gen-value c)))
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
    (st/set-val! (u/get-defins-descr-path p idx) descr)
    (st/set-val! (u/get-defins-class-path p idx) cls)
    (store-conds p idx conds)
    (store-defins p idx defin)
    (st/set-val! (u/get-defins-ctrl-path p idx) "ready")))

(defn store-all-definitions
  "Triggers the storing of the definition section."
  [p {defins :Definitions}]
  (doall
   (map-indexed
    (fn [idx ds] (store-definitions p idx ds))
    defins)))

;;------------------------------
;; meta
;;------------------------------
(defn store-meta
  "Stores the mp meta data."
  [p {standard :Standard
      name :Name
      descr :Description
      cont :Container
      defins :Definitions}]
  (st/set-val! (u/get-meta-std-path p) standard)
  (st/set-val! (u/get-meta-name-path p) name)
  (st/set-val! (u/get-meta-descr-path p) descr)
  (st/set-val! (u/get-meta-ndefins-path p) (count defins))
  (st/set-val! (u/get-meta-ncont-path p) (count cont)))

;;------------------------------
;; all
;;------------------------------
(defn store
  "Triggers the storing of meta, exchange, containers etc. to
  the short term memory. Clears up the fields before.
  ```clojure
  (with-meta todo)
  ```"
  {:example-mp {
  :_id "mpd-wait",
  :_rev "1-7c9116bcfc604970614881843700e3ce",
  :Mp {
    :Container [
      {:Description "Just waits",
       :Ctrl "void",
       :Title "Simple wait", :Element ["*"],
       :Definition [
                    [
                     {:TaskName "Common-wait",
                      :Replace {:#waittime "CDG_Modbus"}
                     }
                    ]
                   ]
      }],
    :Date [{:Type "created",
            :Value "2018-05-31"}],
    :Name "wait",
    :Description "Simplest possible measurement programm definition.",
    :Standard "NN"}}
   }
  [{id :_id
    rev :_rev
    mp :Mp}]
  (let [p (u/extr-main-path id)]
    (st/clear (u/get-meta-prefix p))
    (st/clear (u/get-exch-prefix p))
    (st/clear (u/get-cont-prefix p))
    (st/clear (u/get-defins-prefix p))
    (store-meta p mp)
    (store-exchange p mp)
    (store-all-container p mp)
    (store-all-definitions p mp)))
