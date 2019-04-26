(ns ^{:author "wactbprot"
      :doc "Builds up the short term memory with given the mp-definition."}
    cmp.task
  (:require [cmp.utils :as u]
            [clojure.spec.alpha :as s]
            [cmp.st :as st])
  (:gen-class))

(s/def ::TaskName string?)
(s/def ::Action string?)
(s/def ::Replace map?)
(s/def ::Use map?)
(s/def ::proto-task (s/keys :req-un [::TaskName]
                            :opt-un [::Replace ::Use]))

(s/def ::task (s/keys :req-un [::TaskName ::Action]))

(defn proto-task? [x]
  (assert (s/valid? ::proto-task x)))

(defn task? [x]
  (assert (s/valid? ::task x)))