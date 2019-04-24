(ns ^{:author "wactbprot"
      :doc "Builds up the short term memory with given the mp-definition."}
    cmp.task
  (:require [cmp.utils :as u]
            [clojure.spec.alpha :as s]
            [cmp.st :as st])
  (:gen-class))

(s/def ::TaskName string?)
(s/def ::proto-task (s/keys :req-un [::TaskName]))


(defn compl [proto-task]
  (println proto-task)
  (assert (s/valid? ::proto-task proto-task))
  (println "....")
  )