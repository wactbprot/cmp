(ns cmp.task
  ^{:author "wactbprot"
    :doc "Spec for tasks."}
  (:require [clojure.spec.alpha :as s]
            [taoensso.timbre :as log]
            [clojure.walk :as walk]
            [clojure.string :as string]
            [cmp.utils :as u]
            [cmp.doc :as d]
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

(defmulti task?
  (fn [m] (m :Action)))

(defmethod task? "TCP"
  [m]
  (s/valid? ::tcp-task m))

(defmethod task? :default
  [m]
  (s/valid? ::task m))

(defn proto-task?
  [x]
  (s/valid? ::proto-task x))

(defn meta-task?
  [x]
  (task? (:Task x)))

(defn get-from-exchange
  "
  {
  :%stateblock1 Vraw_block1
  :%stateblock2 Vraw_block2
  :%stateblock3 Vraw_block3
  :%stateblock4 Vraw_block4
  }"
  [m mp-name]
  (u/apply-to-map-values (fn [x] (u/json->map (st/key->val (u/get-exch-path mp-name x)))) m)
  )

(defn from-exchange
  [task]
  (let [mp-name (:MpName task)
        fexch   (:FromExchange task)]
    (cond
      (nil? mp-name) task
      (nil? fexch)   task
      :else (get-from-exchange fexch mp-name)
      )))

(defn ->globals
  "Returns a map with replacements
  of general intrest.

  ```clojure
  (->globals)
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

(defmulti replace-map
  "Replaces tokens (given in the m) in the task.

  ```clojure
  (replace-map (->globals) {:TaskName \"foo\" :Value \"%time\"})
  ;; {:TaskName \"foo\", :Value \"1580652820247\"}
  ```
  "
  (fn [m task] (map? m)))

(defmethod replace-map false
  [m task]
  task)

(defmethod replace-map true 
  [m task]
  (u/json->map (string/replace
                (u/map->json task)
                (u/gen-re-from-map-keys m)
                m)))
  
(defn extract-use-value
  [task m k]
  ((task k) (keyword (m k))))

(defn make-singular-kw
  "Takes a keyword or string and removes the tailing
  letter (most likely a s). Turns the result
  to a keyword.
  
  ```clojure
  (make-singular-kw :Values)
  ;; :Value
  (make-singular-kw \"Values\")
  ;; :Value
  ```
  "
  [s]
  (->> s
       name
       (re-matches #"^(\w*)(s)$")
       second
       keyword))

(defmulti merge-use-map
  "The use keyword enables a replace mechanism.
  It works like this:
  proto-task:

  ```clojure
  Use: {Values: med_range}
  ;; should lead to:
  task: { Value: rangeX.1}
  ```"
  (fn [m task] (map? m)))

(defmethod merge-use-map false
  [m task]
  task)

(defmethod merge-use-map true
  [m task]
  (merge task (into {}
                   (map
                    (fn [k]
                      (hash-map (make-singular-kw k) (extract-use-value task m k)))
                    (keys m)))))

(defmulti gen-meta-task
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
  class)

(defmethod gen-meta-task String
  [task-name]
  (gen-meta-task {:TaskName task-name}))

(defmethod gen-meta-task :default
  [proto-task]
  (let [task-name (:TaskName proto-task)
        db-task   (->> ["tasks" task-name]
                       u/vec->key
                       st/key->val
                       u/json->map)]
    {:Task          (dissoc db-task :Defaults)
     :Use           (:Use proto-task)
     :Globals       (u/make-map-regexable (->globals))
     :Defaults      (u/make-map-regexable (:Defaults db-task))
     :FromExchange  (u/make-map-regexable (from-exchange db-task))
     :Replace       (u/make-map-regexable (:Replace proto-task))}))

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
  **todo**

  FromExchange
  
  "
  [meta-task]
  (let [{task     :Task 
         use-map  :Use
         fromexch :FromExchange
         replace  :Replace
         defaults :Defaults 
         globals  :Globals} meta-task]
     (->> task
          (merge-use-map use-map)
          (replace-map replace)
          (replace-map defaults)
          (replace-map globals))))