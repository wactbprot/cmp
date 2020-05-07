(ns cmp.task
  ^{:author "wactbprot"
    :doc "Spec for tasks."}
  (:require [clojure.spec.alpha :as s]
            [clojure.core.async :as a]
            [taoensso.timbre :as log]
            [clojure.walk :as w]
            [clojure.string :as string]
            [cmp.utils :as u]
            [cmp.doc :as d]
            [cmp.excep :as excep]
            [cmp.exchange :as exch]
            [cmp.lt-mem :as lt]
            [cmp.st-mem :as st]))


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

(defn task?
  "Checks the task structure against a spec."
  [m]
  (if (nil? m)
    (a/>!! excep/ch "task is nil")
    (condp :Action m
      "TCP"    (s/valid? ::tcp-task m)
      :default (s/valid? ::task m))))

(defn proto-task?
  [x]
  (s/valid? ::proto-task x))

(defn meta-task?
  [x]
  (task? (:Task x)))

;;------------------------------
;; action(s)(?)
;;------------------------------
(defn action-eq?
  "A `=` partial on the `task` `:Action`."
  [task]
  {:pre [(map? task)]}
  (partial = (keyword (:Action task))))

(defn dev-action?
  "Device actions are:
  * :MODBUS
  * :VXI11
  * :TCP
  * :UDP 
  "
  [task]
  {:pre [(map? task)]}
  (some (action-eq? task) [:MODBUS :VXI11 :TCP :UDP]))

(defn globals
  "Returns a map with replacements
  of general intrest.

  ```clojure
  (globals)
  ;; {\"%hour\" \"14\",
  ;; \"%minute\" \"07\",
  ;; \"%second\" \"54\",
  ;; \"%year\" \"2020\",
  ;; \"%month\" \"02\",
  ;; \"%day\" \"02\",
  ;; \"%time\" \"1580652474824\"}
  ```
  "
  []
  (let [d (u/get-date-object)]
    {"%hour"   (u/get-hour d)
     "%minute" (u/get-min d)
     "%second" (u/get-sec d)
     "%year"   (u/get-year d)
     "%month"  (u/get-month d)
     "%day"    (u/get-day d)
     "%time"   (u/get-time d)}))


(defn outer-replace-map
  "Replaces tokens (given in the m) in the task.
  This kind of replacement is used during the
  task build up at the beginning of its life cycle.
  
  Example:
  ```clojure
  (outer-replace-map (globals) {:TaskName \"foo\" :Value \"%time\"})
  ;; {:TaskName \"foo\", :Value \"1580652820247\"}
  (outer-replace-map nil {:TaskName \"foo\" :Value \"%time\"})
  ;; {:TaskName \"foo\", :Value \"%time\"}
  ```
  "
  [m task]
  (if (map? m)
    (u/json->map
     (reduce
      (fn [s [k v]]
        (let [pat (re-pattern (name k))
              r   (u/clj->str-val v)]
          (string/replace s pat r)))
      (u/map->json task) m))
    task))

(defn inner-replace-map
  "Applies the generated function  `f` to the
  values `v` of the of the `task` map. `f`s input is `v`.
  If `m` has a key `v` the value of this key is returned.
  If `m` has no key `v` the `v` returned.
  This kind of replacement is used during the
  runtime.
  "
  [m task]
  (let [nm (u/apply-to-map-keys name m)
        f (fn [v]
            (if-let [r (get nm  v)]
              r
              v))]
    (u/apply-to-map-values f task)))


(defn extract-use-value
  [task m k]
  ((keyword (m k)) (task k)))

(defn str->singular-kw
  "Takes a keyword or string and removes the tailing
  letter (most likely a s). Turns the result
  to a keyword.
  
  ```clojure
  (str->singular-kw :Values)
  ;; :Value
  (str->singular-kw \"Values\")
  ;; :Value
  ```
  "
  [s]
  (->> s
       name
       (re-matches #"^(\w*)(s)$")
       second
       keyword))

(defn merge-use-map
  "The use keyword enables a replace mechanism.
  It works like this:
  proto-task:
  
  ```clojure
  Use: {Values: med_range}
  ;; should lead to:
  task: { Value: rangeX.1}
  ```"
  [m task]
  (if (map? m)
    (merge task
           (into {}
                 (map
                  (fn [k]
                    (hash-map
                     (str->singular-kw k)
                     (extract-use-value task m k)))
                  (keys m))))
    task))

(defn ensure-proto-task
  "Returns x if it is not a string."
  [x]
  (if (string? x)
    {:TaskName x}
    x))

(defn gen-meta-task
  "Gathers all information for the given `proto-task` (map).
  The `proto-task` should be a map containing the `:TaskName`
  `keyword` at least. String version makes a `map` out of `s`
  and calls related method.

  ```clojure
  (gen-meta-task \"Common-wait\")
  ;; 19-12-27 11:14:48 hiob DEBUG [cmp.lt-mem:21] - get task:  Common-wait  from ltm
  ;; {:Task
  ;; {:Action \"wait\",
  ;; :Comment \"%waitfor  %waittime ms\",
  ;; :TaskName \"Common-wait\",
  ;; :WaitTime \"%waittime\"},
  ;; :Use nil,
  ;; :Defaults
  ;; {\"%unit\" \"mbar\",
  ;; \"%targetdb\" \"vl_db\",
  ;; \"%relayinfo\" \"relay_info\",
  ;; \"%docpath\" \"\",
  ;; \"%sourcedb\" \"vl_db_work\",
  ;; \"%timepath\" \"Time\",
  ;; \"%waitunit\" \"ms\",
  ;; \"%break\" \"no\",
  ;; \"%waitfor\" \"Ready in\",
  ;; \"%waittime\" \"1000\",
  ;; \"%dbinfo\" \"db_info\"},
  ;; :Globals
  ;; {\"%hour\" \"11\",
  ;; \"%minute\" \"14\",
  ;; \"%second\" \"48\",
  ;; \"%year\" \"2019\",
  ;; \"%month\" \"12\",
  ;; \"%day\" \"27\",
  ;; \"%time\" \"1577445288247\"},
  ;; :Replace nil}
  ;;
  ;; call the map vesion as follows:
  
  (gen-meta-task {:TaskName \"Common-wait\" :Replace {\"%waittime\" 10}})
  ```"
  [x]
  (let  [proto-task (ensure-proto-task x)
         task-name  (:TaskName proto-task)
         db-task    (merge
                     (->> ["tasks" task-name]
                          u/vec->key
                          st/key->val)
                     proto-task)]
    {:Task          (dissoc db-task :Defaults) 
     :Use           (:Use proto-task)
     :Globals       (globals)
     :Defaults      (:Defaults db-task)
     :Replace       (:Replace proto-task)}))

(defn assemble
  "Assembles the `task` from the given
  `meta-task` in a special order:

  * merge Use
  * replace from Replace
  * replace from Defaults
  * replace from Globals
  
  `assoc`s the structs afterwards.

  ```clojure
  (assemble
    (gen-meta-task {:TaskName \"Common-wait\"
                    :Replace {\"%waittime\" 10}}))
  ;; {:Action \"wait\",
  ;;  :Comment \"Ready in  10 ms\",
  ;;  :TaskName \"Common-wait\",
  ;;  :WaitTime \"10\",
  ;;  ...
  ;; }
  ```
  "
  [meta-task]
  (let [db-task  (:Task         meta-task)
        use-map  (:Use          meta-task)
        replace  (:Replace      meta-task)
        defaults (:Defaults     meta-task)
        globals  (:Globals      meta-task)
        mp-name  (:MpName       meta-task)
        state-k  (:StateKey     meta-task)
        exch-map (:FromExchange db-task)
        task     (dissoc db-task
                         :FromExchange
                         :Replace)
        from-exch-map (exch/from! mp-name exch-map)]
    (assoc 
     (->> task
          (merge-use-map use-map)
          (inner-replace-map from-exch-map)
          (outer-replace-map replace)
          (outer-replace-map defaults)
          (outer-replace-map globals))
     :MpName    mp-name
     :StateKey  state-k)))
