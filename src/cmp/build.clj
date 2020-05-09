(ns cmp.build
  ^{:author "wactbprot"
    :doc "Builds up the short term memory with given the `mpd`."}
  (:require [cmp.utils :as u]
            [cmp.st-mem :as st]
            [taoensso.timbre :as timbre]))

;;------------------------------
;; exchange
;;------------------------------
(defn store-exchange
  "Stores the exchange data."
  [p {exchange :Exchange}]
  (doseq [[k v] exchange]
    (st/set-val! (st/exch-path p (name k)) v)))

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
          (st/set-val! (st/cont-defin-path p idx jdx kdx)  ptsk)
          (st/set-val! (st/cont-state-path p idx jdx kdx) "ready"))
        s)))
    defin)))

(defn store-container
  "Stores a single container"
  [p idx {descr :Description
          title :Title
          ctrl  :Ctrl
          elem  :Element
          defin :Definition}]           
  (st/set-val! (st/cont-title-path p idx) title)
  (st/set-val! (st/cont-descr-path p idx) descr)
  (st/set-val! (st/cont-ctrl-path p idx) ctrl)
  (st/set-val! (st/cont-elem-path p idx) elem)
  (store-defin p idx defin))

(defn store-all-container
  "Triggers the storing of the singel containers"
  [p {conts :Container}]
  (doall
   (map-indexed
    (fn [idx cont] (store-container p idx cont))
    conts)))

;;------------------------------
;; definitions
;;------------------------------
(defn store-defins
  "Stores the definitions section."
  [p idx defin]
  (doall
   (map-indexed
    (fn [jdx s]
      (doall
       (map-indexed
        (fn [kdx ptsk]
          (st/set-val! (st/defins-defin-path p idx jdx kdx) ptsk)
          (st/set-val! (st/defins-state-path p idx jdx kdx) "ready"))
        s)))
    defin)))

(defn store-conds
  "Stores the definitions conditions."
  [p idx conds]
  (doall
   (map-indexed
    (fn [jdx c]
      (st/set-val! (st/defins-cond-path p idx jdx) c))
        conds)))

(defn store-definitions
  "Stores a definition given in the definition section
  (second way beside container to provide definitions).
  This includes `DefinitionClass` and `Conditions`."
  [p idx ds]
  (let [{cls :DefinitionClass
         descr :ShortDescr
         conds :Condition
         defin :Definition} ds]
    (st/set-val! (st/defins-descr-path p idx) descr)
    (st/set-val! (st/defins-class-path p idx) cls)
    (store-conds p idx conds)
    (store-defins p idx defin)
    (st/set-val! (st/defins-ctrl-path p idx) "ready")))

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
  "Stores the mp meta data."
  [p {standard :Standard
      name :Name
      descr :Description
      cont :Container
      defins :Definitions}]
  (st/set-val! (st/meta-std-path p) standard)
  (st/set-val! (st/meta-name-path p) name)
  (st/set-val! (st/meta-descr-path p) descr)
  (st/set-val! (st/meta-ndefins-path p) (count defins))
  (st/set-val! (st/meta-ncont-path p) (count cont)))

;;------------------------------
;; all
;;------------------------------
(defn store
  "Triggers the storing of `meta`, `exchange`,
  `container`s etc. to the short term memory.
  Clears up the fields before. 
  ```clojure
  ;; use metadata example input
  (store ((meta (var store)) :example-input))
  ```"
  {:example-input (read-string
                   (slurp "resources/mpd-ref.edn"))}
  [{id :_id rev :_rev mp :Mp}]
  (let [p (u/main-path id)]
    (st/clear (st/meta-prefix p))
    (st/clear (st/exch-prefix p))
    (st/clear (st/cont-prefix p))
    (st/clear (st/defins-prefix p))
    (store-meta p mp)
    (store-exchange p mp)
    (store-all-container p mp)
    (store-all-definitions p mp)))

(defn store-task
  "Stores the given `task` unter the
  path `tasks@<TaskName>`."
  [task]
  (st/set-val!
   (u/vec->key ["tasks" (:TaskName task)])
   (u/doc->safe-doc task)))

(defn store-tasks
  "Stores the `task-list`
  as received from `lt-mem`."
  [task-list]  
  (run!
   (fn [{task :value}]
     (store-task task))
   task-list))
