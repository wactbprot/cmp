(ns ^{:author "wactbprot"
      :doc "Builds up the short term memory with given the mp-definition."}
    cmp.build
  (:require [cmp.utils :as utils]
            [cmp.st :as st])
  (:gen-class))

(defn distrib-exchange [path {exchange :Exchange}]
  (doseq [[k v] exchange]
    (st/set-val (utils/gen-key [path "exchange" (name k)])
            (utils/gen-value v))))

(defn distrib-definition [path {definition :Definition}]
  (doall
   (map-indexed
    (fn [j s]
      (doall
       (map-indexed
        (fn [k p]
          (st/set-val (utils/gen-key [path "definition" j k])
                      (utils/gen-value p)))
        s)))
   definition)))

(defn distrib-containers [path {container :Container}]
  (doall
   (map-indexed
    (fn [i c]
      (let [{description :Description
             title :Title
             ctrl :Ctrl
             elem :Element
             definition :Definition} c
            e-path "container"]           
        (st/set-val (utils/gen-key [path e-path i "title"])
                    (str title))
        (st/set-val (utils/gen-key [path e-path i "description"])
                    (str description))
        (st/set-val (utils/gen-key [path e-path i "ctrl"])
                    (utils/gen-value ctrl))
        (st/set-val (utils/gen-key [path e-path i "elem"])
                    (utils/gen-value elem))
        (distrib-definition (utils/gen-key [path e-path i]) c)
       ))
    container)))

(defn distrib [{id :_id rev :_rev mp-def :Mp}]
  (let [path (utils/extr-main-path id)]
    (st/clear [path "exchange"])
    (distrib-exchange path mp-def)
    (st/clear [path "container"])
    (distrib-containers path mp-def)))
