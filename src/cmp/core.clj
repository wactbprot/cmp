(ns cmp.core
  (:require [cmp.lt :as lt]
            [cmp.st :as st])
  (:gen-class))

(defn main
  "Loads document from long term memory and fetches it to short term memory"
  [id]
    (st/distrib (lt/get-document id)))
  