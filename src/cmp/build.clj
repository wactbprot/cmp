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
  [path {exchange :Exchange}]
  (doseq [[k v] exchange]
    (st/set-val! (u/vec->key [path "exchange" (name k)])
            (u/gen-value v))))

(defn store-definition
  "Stores the definition section."
  [path {definition :Definition}]
  (doall
   (map-indexed
    (fn [idx s]
      (doall
       (map-indexed
        (fn [jdx p]
          (let [st-path (u/vec->key [path "definition" idx jdx])
                st-value (u/gen-value p)]
            (log/info "try to write proto task to path: " st-path)
            (log/debug "proto task is:" p)
            (assert (tsk/proto-task? p))
            (st/set-val! st-path st-value)))
        s)))
   definition)))

(defn store-containers
  "Stores the containers"
  [path {container :Container}]
  (doall
   (map-indexed
    (fn [i c]
      (let [{description :Description
             title :Title
             ctrl :Ctrl
             elem :Element
             definition :Definition} c
            e-path "container"]           
        (st/set-val! (u/vec->key [path e-path i "title"])
                     title)
        (st/set-val! (u/vec->key [path e-path i "description"])
                     description)
        (st/set-val! (u/vec->key [path e-path i "ctrl"])
                     (u/gen-value ctrl))
        (st/set-val! (u/vec->key [path e-path i "elem"])
                     (u/gen-value elem))
        (store-definition (u/vec->key [path e-path i]) c)
        ))
    container)))

(defn store-meta
  "Stores the mp meta data."
  [path {standard :Standard name :Name descr :Describtion}]
  (let [e-path "meta"]
    (st/set-val! (u/vec->key [path e-path "standard"])
                 standard)
    (st/set-val! (u/vec->key [path e-path "name"])
                 name)
    (st/set-val! (u/vec->key [path e-path "description"])
                 descr)))

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
