(ns cmp.build
  (:require [cmp.utils :as utils]
            [cmp.st :as st])
  (:gen-class))

(defn distrib-exchange [path {exchange :Exchange}]
  (doseq [[k v] exchange]
    (st/set-val (utils/gen-key [path "exchange" (name k)])
            (utils/gen-value v))))

(defn distrib-definition [path {definition :Definition}]
  (map-indexed
   (fn [j s]
     (map-indexed
      (fn [k p]
        (st/set-val (utils/gen-key [path "definition" j k])
                (utils/gen-value p))
        (st/set-val (utils/gen-key [path "state" j k])
                "build"))
      s))
   definition))

(defn distrib-containers [path {container :Container}]
  (def e-path "container")
  (map-indexed
   (fn [i c]
     (let [{description :Description
            title :Title
            ctrl :Ctrl
            elem :Element
            definition :Definition} c]           
       (st/set-val (utils/gen-key [path e-path i "title"])
               (str title))
       (st/set-val (utils/gen-key [path e-path i "description"])
               (str description))
       (st/set-val (utils/gen-key [path e-path i "ctrl"])
               (utils/gen-value ctrl))
       (st/set-val (utils/gen-key [path e-path i "elem"])
               (utils/gen-value elem))
       (distrib-definition (utils/gen-key [path e-path i]) c)))
   container))

(defn distrib [{id :_id rev :_rev mp-def :Mp}]
  (let [path (utils/extr-main-path id)]
    (st/clear [path "exchange"])
    (distrib-exchange path mp-def)
    (st/clear [path "container"])
    (distrib-containers path mp-def)))
