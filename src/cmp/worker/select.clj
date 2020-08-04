(ns cmp.worker.select
  ^{:author "wactbprot"
    :doc "Worker selects a definition from the same `mp-id` 
          by evaluating the related conditions.
         "}
  (:require [taoensso.timbre :as log]
            [clojure.string :as string]
            [cmp.st-mem :as st]
            [cmp.exchange :as exch]
            [cmp.utils :as u]
            [cmp.config :as cfg]))

(def mtp (cfg/min-task-period (cfg/config)))

(defn cond-match?
  "Tests a single condition of the form defined in
  the `definitions` section.
  
  **Example**
  
  Condition maps `m` looks like this:
  
  ```clojure
  (u/json->map (st/key->val \"ref@definitions@0@cond@0\"))
  ;;
  ;; {
  ;;  :ExchangePath \"A.Unit\" ; the value under ref@exchange@A/Unit
  ;;  :Methode \"eq\"          ; should be equal to
  ;;  :Value \"Pa\"            ; Pa
  ;; }
  ```
  "
  [k]
  (let [mp-id (st/key->mp-id k)
        m     (st/key->val    k)
        p     (:ExchangePath m)
        a     (str (exch/comp-val mp-id p))
        b     (str (:Value m))]
    (condp = (keyword (:Methode m))
      :eq (= a b)
      :lt (< (read-string a) (read-string b))
      :gt (> (read-string a) (read-string b)))))

(defn conds-match?
  "Gathers all information for the given
  definitions key comparison and checks
  all of the found conditions.
  
  ```clojure
  (conds-match? \"ref@definitions@0@class\")
  ;; false
  (conds-match? \"ref@definitions@1@class\")
  ;; true

  ```
  "
  [k]
  (let [mp-id    (st/key->mp-id k)
        no-idx   (st/key->no-idx k)
        pat      (u/vec->key [mp-id "definitions" no-idx "cond@*"])
        cond-ks  (st/pat->keys pat)
        match-ks (filter cond-match? cond-ks)] 
    (=
     (count cond-ks)
     (count match-ks))))

(defn start-defins!
  "Starts the matching `definitions` structure. `register`s
  a level b callback. Sets the state of the calling element to `executed`
  if the `ctrl`  turns to ready (or error if error).

  "          
  [match-k state-k]
  (log/debug "start definitions struct " match-k)
  (let [struct   "definitions"
        func     "ctrl"
        mp-id    (st/key->mp-id state-k)
        no-idx   (st/key->no-idx match-k)
        ctrl-k   (st/defins-ctrl-path mp-id no-idx)
        level    "b"
        callback  (fn [msg]
                    (condp = (keyword (st/key->val ctrl-k))
                      :run   (log/debug "run callback for" ctrl-k)
                      :ready (do
                               (log/debug "ready callback for" ctrl-k)
                               (when state-k
                                 (Thread/sleep mtp)
                                 (st/set-val! state-k "executed")) 
                               (st/de-register! mp-id struct no-idx func level))
                      :error (do
                               (log/error "error callback for" ctrl-k)
                               (when state-k
                                 (Thread/sleep mtp)
                                 (st/set-val! state-k "error")))))]
    (st/register! mp-id struct no-idx func callback level)
    (st/set-val! ctrl-k "run")))

(defn cond-key->cond-map
  [k]
  (when k
    {:mp-name (st/key->mp-id k)
     :struct (st/key->struct k)
     :no-idx (st/key->no-idx k)
     :cond-expr (st/key->val k)}))

(defn ks->cond-vec
  [ks]
  (when ks
    (mapv cond-key->cond-map ks)))

(defn select-definition!
  "Selects and runs a `Definition` from the `Definitions`
  section of the current `mp`. 
  
  ```clojure
  (ns cmp.worker.select)
  (select-definition! {:Action select
                       :TaskName Common-select_definition,
                       :DefinitionClass wait} )
  ```
  If more than one than one of the definitions condition match
  the first is used:

  ```clojure
  (first (filter conds-match? match-ks))
  ;; ref@definitions@1@class
  ```" 
  [{mp-id :MpName cls :DefinitionClass state-key :StateKey}]
  (st/set-val! state-key "working")
  (log/debug "start with select, already set " state-key  " working")
  (Thread/sleep mtp)
  (let [pat   (u/vec->key [mp-id "definitions" "*" "class"])
        ks    (sort (st/filter-keys-where-val pat cls))]
    (prn (ks->cond-vec ks))
    ;; cond-vec mit ersetzten exchange values erzeugen
    ;; und side effect frei untersuchen
    (if-let [k (first (filter conds-match? ks))]
      (start-defins! k state-key)
      (do
        (log/error "nothing match")
        (st/set-val! state-key "error")))))
