(ns cmp.task
  ^{:author "wactbprot"
    :doc "Builds up the short term 
          memory with given the mp-definition."}
  (:require [clojure.spec.alpha :as s]
            [taoensso.timbre :as log]
            [clojure.data.json :as json]
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

(defn global-defaults
  []
  (let [d (u/get-date-object)]
    {"%hour" (u/get-hour d)
     "%minute" (u/get-min d)
     "%second" (u/get-sec d)
     "%year" (u/get-year d)
     "%month" (u/get-month d)
     "%day" (u/get-day d)
     "%time" (u/get-time d)}))

(defn get-temps
  "Temps contain values related to the current mpd."
  [p]
  {"%standard" (st/key->val (u/vec->key [p "meta" "standard"]))
   "%mpname" (st/key->val (u/vec->key [p "meta" "name"]))})

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
  (let [{replace :Replace use :Use} proto-task
        {db-task :value} (u/doc->safe-doc (lt/get-task-view proto-task))
        {defaults :Defaults} db-task
        task (dissoc db-task :Defaults)
        globals (global-defaults)]
    {:Task task
     :Use use
     :Defaults (u/make-map-regexable defaults)
     :Globals (u/make-map-regexable globals)
     :Replace (u/make-map-regexable replace)}))

(defn assemble
  "Assembles the `task` from the given
  `meta-task` in a special order.
  `assoc`s the structs afterwards."
  [meta-task]
  (let [{task :Task 
         use-map :Use 
         replace :Replace
         defaults :Defaults 
         globals :Globals} meta-task]
    (assoc
     (->> task
          (merge-use-map use-map)
          (replace-map replace)
          (replace-map defaults)
          (replace-map globals))
     :Defaults defaults
     :Globals globals
     :Use use-map
     :Replace replace)))