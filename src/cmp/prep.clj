(ns cmp.prep
  (:require [cmp.utils :as u]
            [cmp.st :as st]
            [cmp.task :as tsk]
            [cmp.lt :as lt]
            [taoensso.timbre :as log])
  (:use [clojure.repl])
  (:gen-class))

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
        globals (tsk/global-defaults)]
    {:Task task
     :Use use
     :Customer (not (nil? cust))
     :Defaults (u/make-map-regexable  defaults)
     :Globals (u/make-map-regexable globals)
     :Replace (u/make-map-regexable replace)
     }))

(defn get-temps
  "Temps contain values related to the current mpd.
   Reminder: customer tasks; e.g. the @devicename key belongs
   to Customer=true"
  [p]
    ;;; def["@devicename"] = dn;
    ;;; def["@cdids"]      = idArr;
  {"@standard" (st/get-val (u/gen-key [p "meta" "standard"]))
   "@mpname" (st/get-val (u/gen-key [p "meta" "name"]))})        

(defn container [p i]
  (let [path [p "container" i "definition"]
        ks (st/get-keys (u/gen-key path ))]
    (doall 
     (map
      (fn [k]
        (let [state-key (u/replace-key-at-level 3 k "state")
              proto-task (u/gen-map (st/get-val k))
              meta-task (assoc (gen-meta-task proto-task) :Temps (get-temps p))]
          (assert (tsk/task? (:Task meta-task)))
          (st/set-val! state-key "prepairing")
          (tsk/assemble meta-task)))
      ks))))
