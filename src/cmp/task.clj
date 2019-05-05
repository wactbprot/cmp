(ns cmp.task
  ^{:author "wactbprot"
    :doc "Builds up the short term memory with given the mp-definition."}
  (:require [cmp.utils :as u]
            [clojure.spec.alpha :as s]
            [taoensso.timbre :as log]
            [clojure.data.json :as json]
            [clojure.walk :as walk]
            [clojure.string :as string]
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

(defn global-defaults []
    ;;; def["@devicename"] = dn;
    ;;; def["@cdids"]      = idArr;
  (let [d (u/get-date-object)
        g {"@hour" (u/get-hour d)
           "@minute" (u/get-min d)
           "@second" (u/get-sec d)
           "@year" (u/get-year d)
           "@month" (u/get-month d)
           "@day" (u/get-day d)
           "@time" (u/get-time d)
           }]
    g))

(defn replace-map
  "Replaces tokens (given in the m) in the task."
  [task m]
  (if m
    (let [task-s (u/gen-value task)
          re-k (u/gen-re-from-map-keys m)]
      (u/gen-map (string/replace task-s re-k  m-v)))
    task))

;; (defn assemble
;;   "Assembles the task from different sources in a certain order.
;;   Reminder: customer tasks; e.g. the @devicename key belongs
;;   to Customer=true"
;;   [meta-task]
;;   (let []
;;     ;; assoc globals to defaults
;;     (replace-map task defaults)))
