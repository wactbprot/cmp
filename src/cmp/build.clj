(ns cmp.build
  ^{:author "wactbprot"
    :doc "Builds up the short term memory with given the `mpd`."}
  (:require [com.brunobonacci.mulog  :as mu]
            [cmp.st-mem              :as st]
            [cmp.key-utils           :as stu]
            [cmp.utils               :as u]))

;;------------------------------
;; exchange
;;------------------------------
(defn store-exchange
  "Stores the exchange data."
  [p {exchange :Exchange}]
  (doseq [[k v] exchange]
    (st/set-val! (stu/exch-key p (name k)) v)))

;;------------------------------
;; container
;;------------------------------
(defn store-defin
  "Stores the definition section."
  [p idx defin]
  (doall
   (map-indexed
    (fn [jdx s]
      (doall
       (map-indexed
        (fn [kdx ptsk]
          (st/set-val! (stu/cont-defin-key p idx jdx kdx)  ptsk)
          (st/set-val! (stu/cont-state-key p idx jdx kdx) "ready"))
        s)))
    defin)))

(defn store-container
  "Stores a single container."
  [p idx {descr :Description
          title :Title
          ctrl  :Ctrl
          elem  :Element
          defin :Definition}]
  (st/set-val! (stu/cont-title-key p idx) title)
  (st/set-val! (stu/cont-descr-key p idx) descr)
  (st/set-val! (stu/cont-ctrl-key p idx) ctrl)
  (st/set-val! (stu/cont-elem-key p idx) elem)
  (store-defin p idx defin))

(defn store-all-container
  "Triggers the storing of the containers."
  [p {conts :Container}]
  (doall
   (map-indexed
    (fn [idx cont] (store-container p idx cont))
    conts)))

;;------------------------------
;; definitions
;;------------------------------
(defn store-defins
  "Stores the definitions section. Initiates all state keys with
  `ready`."
  [p idx defin]
  (doall
   (map-indexed
    (fn [jdx s]
      (doall
       (map-indexed
        (fn [kdx ptsk]
          (st/set-val! (stu/defins-defin-key p idx jdx kdx) ptsk)
          (st/set-val! (stu/defins-state-key p idx jdx kdx) "ready"))
        s)))
    defin)))

(defn store-conds
  "Stores the definitions conditions."
  [p idx conds]
  (doall
   (map-indexed
    (fn [jdx c]
      (st/set-val! (stu/defins-cond-key p idx jdx) c))
        conds)))

(defn store-definitions
  "Stores a definition given in the definition section
  (second way beside container to provide definitions).  This includes
  `DefinitionClass` and `Conditions`."
  [p idx ds]
  (let [{cls   :DefinitionClass
         descr :ShortDescr
         conds :Condition
         defin :Definition} ds]
    (st/set-val! (stu/defins-descr-key p idx) descr)
    (st/set-val! (stu/defins-class-key p idx) cls)
    (store-conds p idx conds)
    (store-defins p idx defin)
    (st/set-val! (stu/defins-ctrl-key p idx) "ready")))

(defn store-all-definitions
  "Triggers the storing of the definition section."
  [p {defins :Definitions}]
  (doall
   (map-indexed
    (fn [idx ds] (store-definitions p idx ds))
    defins)))

;;------------------------------
;; meta
;;------------------------------
(defn store-meta
  "Stores the meta data of an mpd:

  * standard
  * name
  * description
  * number of containers
  * number of definitions
  "
  [p {standard :Standard
      name     :Name
      descr    :Description
      cont     :Container
      defins   :Definitions}]
  (st/set-val! (stu/meta-std-key p) standard)
  (st/set-val! (stu/meta-name-key p) name)
  (st/set-val! (stu/meta-descr-key p) descr)
  (st/set-val! (stu/meta-ndefins-key p) (count defins))
  (st/set-val! (stu/meta-ncont-key p) (count cont)))

;;------------------------------
;; all
;;------------------------------
(defn store
  "Triggers the storing of `meta`, `exchange`, `container`s etc. to the
  short term memory."
  [{id :_id rev :_rev mp :Mp}]
  (let [p (u/extr-main-path id)]
    (store-meta p mp)
    (store-exchange p mp)
    (store-all-container p mp)
    (store-all-definitions p mp)))

;;------------------------------
;; tasks
;;------------------------------
(defn store-task
  "Stores the given `task` unter the path `tasks@<TaskName>`."
  [task]
  (st/set-val! (stu/task-key (:TaskName task)) (u/doc->safe-doc task)))

(defn store-tasks
  "Stores the `task-list` as received
  from `lt-mem`."
  [task-list]
  (run!
   (fn [{task :value}] (store-task task))
   task-list))
