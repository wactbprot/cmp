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

(defn store-definition
  "Stores the definition section."
  [p i defin]
  (doall
   (map-indexed
    (fn [idx s]
      (doall
       (map-indexed
        (fn [jdx ptsk]
          (st/set-val! (u/get-defin-path p i idx jdx) (u/gen-value ptsk)))
        s)))
    defin)))

(defn store-container
  [p i cont]
  (let [{descr :Description
         title :Title
         ctrl :Ctrl
         elem :Element
         defin :Definition} cont
        ep "container"]           
    (st/set-val! (u/vec->key [p ep i "title"]) title)
    (st/set-val! (u/vec->key [p ep i "description"]) descr)
    (st/set-val! (u/vec->key [p ep i "ctrl"]) (u/gen-value ctrl))
    (st/set-val! (u/vec->key [p ep i "elem"]) (u/gen-value elem))
    (store-definition p i defin)))

(defn store-containers
  "Stores the containers"
  [p {conts :Container}]
  (doall
   (map-indexed
    (fn [i cont] (store-container p i cont))
    conts)))

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
  [{id :_id rev :_rev mp-def :Mp}]
  (let [path (u/extr-main-path id)]
    (st/clear [path "meta"])
    (store-meta path mp-def)
    (st/clear [path "exchange"])
    (store-exchange path mp-def)
    (st/clear [path "container"])
    (store-containers path mp-def)))
