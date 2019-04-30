(ns cmp.task
  ^{:author "wactbprot"
    :doc "Builds up the short term memory with given the mp-definition."}
  (:require [cmp.utils :as u]
            [clojure.spec.alpha :as s]
            [taoensso.timbre :as log]
            [clojure.data.json :as json]
            [clojure.string :as string]
            [clojure.walk :as walk]
            [cmp.st :as st])
  (:gen-class))

(s/def ::TaskName string?)
(s/def ::Action string?)
(s/def ::Replace map?)
(s/def ::Use map?)
(s/def ::Host string?)
(s/def ::Port string?)
(s/def ::Value string?)
(s/def ::DocPath string?)
(s/def ::proto-task (s/keys :req-un [::TaskName]
                            :opt-un [::Replace ::Use]))
(s/def ::task (s/keys :req-un [::TaskName ::Action]))
(s/def ::tcp-task (s/keys :req-un [::TaskName ::Host ::Port]
                            :opt-un [::DocPath]))

(defn proto-task? [x]
  (s/valid? ::proto-task x))

(defmulti task?
  (fn [m] (m :Action)))

(defmethod task? "TCP" [m]
  (s/valid? ::tcp-task m))

(defmethod task? :default [m]
  (s/valid? ::task m))

(defn replace-map-in-task
  "Replaces tokens (given in the m) in the task"
  ([task m]
   (let [str-task (json/write-str task)
         re-keys (u/gen-re-from-map-keys m)]
     (string/replace str-task re-keys m))))


(defn assemble
  "Assembles the task from different sources in a certain order."
  [db-task proto-task]
  (let [{replace :Replace use :Use} proto-task
        {defaults :Defaults} db-task
        task (dissoc db-task :Defaults)
        repl-map- (walk/stringify-keys defaults)]
    (replace-map-in-task task defaults)))
