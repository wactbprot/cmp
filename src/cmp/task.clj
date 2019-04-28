(ns cmp.task
  ^{:author "wactbprot"
    :doc "Builds up the short term memory with given the mp-definition."}
  (:require [cmp.utils :as u]
            [clojure.spec.alpha :as s]
            [clojure.data.json :as json]
            [clojure.string :as string]
            [clojure.walk :as walk]
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

(defn task? [x] ;; how to dispatch on :Action
  (assert (s/valid? ::task x)))

(defn gen-re-from-map-keys [m]
  (let [ks (keys m)
        sep "|"]
    (re-pattern (string/join sep ks))))

(defn replace-map-in-task [task m]
  "Replaces tokens (given in the m) in the task"
  (let [str-task (json/write-str task)
        re-keys (gen-re-from-map-keys m)]
    (string/replace str-task re-keys m)))

(defn assemble [db-task proto-task]
  (let [{replace :Replace use :Use} proto-task
        {symb-defaults :Defaults} db-task
        task (dissoc db-task :Defaults)
        defaults (walk/stringify-keys symb-defaults)]
    (replace-map-in-task task defaults)
    )
  )