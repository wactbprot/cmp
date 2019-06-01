(ns cmp.task
  ^{:author "wactbprot"
    :doc "Builds up the short term memory with given the mp-definition."}
  (:require [cmp.utils :as u]
            [clojure.spec.alpha :as s]
            [taoensso.timbre :as log]
            [clojure.data.json :as json]
            [clojure.walk :as walk]
            [clojure.string :as string]
            [cmp.lt :as lt]
            [cmp.st :as st])
  (:gen-class))

(log/set-level! :debug)

(s/def ::TaskName string?)
(s/def ::Action string?)
(s/def ::Replace map?)
(s/def ::Use map?)
;(s/def ::Customer boolean?))
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

(defn get-temps
  "Temps contain values related to the current mpd.
   Reminder: customer tasks; e.g. the @devicename key belongs
   to Customer=true"
  [p]
    ;;; def["@devicename"] = dn;
    ;;; def["@cdids"]      = idArr;
  {"@standard" (st/get-val (u/vec->key [p "meta" "standard"]))
   "@mpname" (st/get-val (u/vec->key [p "meta" "name"]))})        

(defmulti replace-map
  "Replaces tokens (given in the m) in the task."
  (fn [m task] (map? m)))

(defmethod replace-map false
  [m task]
  task)

(defmethod replace-map true 
  [m task]
  (let [task-s (u/gen-value task)
        re-k (u/gen-re-from-map-keys m)]
    (u/gen-map (string/replace task-s re-k m))))

(defn extract-use-value
  [task m k]
  ((task k) (keyword (m k))))

(defn make-singular-kw
  [k]
  (->> k
       (name)
       (re-matches #"^(\w*)(s)$")
       (second)
       (keyword)))

(defmulti merge-use-map
  "The use keyword enables a replace mechanism. It works like this:
  proto-task: Use: {Values: med_range}
  -->
  task: Value: rangeX.1"
  (fn [m task] (map? m)))

(defmethod merge-use-map false
  [m task]
  task)

(defmethod merge-use-map true
  [m task]
  (merge
   task
   (into {}
         (map
          (fn [k]
            (hash-map (make-singular-kw k) (extract-use-value task m k)))
          (keys m))
         )))

(defmulti gen-meta-task
  "Gathers all information for the given proto-task (map).
  The proto-task should be a map containing the TaskName
  keyword at least. Strin version makes a map out of s and
  calls related method (intendet for repl use)."
  class)

(defmethod gen-meta-task String
  [s]
  (gen-meta-task {:TaskName s}))

(defmethod gen-meta-task clojure.lang.PersistentArrayMap
  [proto-task]
  (let [{replace :Replace use :Use cust :Customer} proto-task
        {db-task :value} (lt/get-task-view proto-task)
        {defaults :Defaults} db-task
        task (dissoc db-task :Defaults)
        globals (global-defaults)]
    {:Task task
     :Use use
     :Customer (not (nil? cust))
     :Defaults (u/make-map-regexable defaults)
     :Globals (u/make-map-regexable globals)
     :Replace (u/make-map-regexable replace)
     }))

(defn assemble
  "Assembles the task from the given meta-task in a special order."
  [meta-task]
  (let [{task :Task 
         use-map :Use 
         temps :Temps
         defaults :Defaults 
         globals :Globals
         replace :Replace
         cust? :Customer} meta-task]
    (->> task
         (merge-use-map use-map)
         (replace-map replace)
         (replace-map defaults)
         (replace-map temps)
         (replace-map defaults)
         (replace-map globals))))
