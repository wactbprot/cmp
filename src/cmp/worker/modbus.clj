(ns cmp.worker.modbus
  ^{:author "wactbprot"
    :doc "modbus worker."}
  (:require [taoensso.timbre :as timbre]
            [clojure.core.async :as a]
            [cmp.st-mem :as st]
            [cmp.excep :as excep]
            [cmp.utils :as u]
            [cmp.config :as cfg]))

(def mtp (cfg/min-task-period (cfg/config)))

(defn set-valve-pos
  "In order to avoid starting up a nodeserver to
  execute the following pre script, this is a clojure
  translation.

  To make this working a `Input` like:

  ```javascript
  Input: {
         should:%should,
         valve: %valve,
         stateblock1:%stateblock1,
         stateblock2:%stateblock2,
         stateblock3:%stateblock3,
         stateblock4:%stateblock4
        }
  ```

  has to be added to the task.

  ```javascript
   var valvestates = {};,
   if(%stateblock1) valvestates['1'] = %stateblock1;
   if(%stateblock2) valvestates['2'] = %stateblock2;
   if(%stateblock3) valvestates['3'] = %stateblock3;
   if(%stateblock4) valvestates['4'] = %stateblock4;
   var block =  {'V1': '1', 'V2': '1','V3': '1','V4': 1, 
   'V5': 2, 'V6': 2, 'V7': 2, 'V8': 2, 
   'V9': 3, 'V10': 3, 'V11': 3, 'V12': 3, 
   'V17': 4, 'V18': 4, 'V19': 4, 'V20': 4};
   var vs = valvestates[block['%valve']];
   var vpos = {'V1':0, 'V2':2, 'V3':4, 'V4':6,
   'V5':0, 'V6':2, 'V7':4, 'V8':6,
   'V9':4, 'V10':2, 'V11':0, 'V12':6,
   'V17':0, 'V18':1, 'V19':2, 'V20':3,
   };
   var wa = {'V1':40003, 'V2':40003, 'V3':40003, 'V4': 40003,
   'V5':40004,'V6':40004,'V7':40004,'V8':40004,
   'V9':40005,'V10':40005,'V11':40005,'V12':40005,
   'V17':40007,'V18':40007,'V19':40007,'V20':40007};
   var oc = {'open':1, 'close':0};
   var ad = wa['%valve'];
   vs[vpos['%valve']] = oc['%should'];
   var ret = {'Value' : vs, 'Address':ad}; ret;
  ```  
  "
  [task input state-key]
  (let [blk  {:V1  1     :V2  1     :V3  1     :V4  1 
              :V5  2     :V6  2     :V7  2     :V8  2 
              :V9  3     :V10 3     :V11 3     :V12 3
              :V17 4     :V18 4     :V19 4     :V20 4}
        vpos {:V1  0     :V2  2     :V3  4     :V4  6 
              :V5  0     :V6  2     :V7  4     :V8  6 
              :V9  0     :V10 2     :V11 4     :V12 6
              :V17 0     :V18 1     :V19 2     :V20 3}
        adr  {:V1  40003 :V2  40003 :V3  40003 :V4  40003
              :V5  40004 :V6  40004 :V7  40004 :V8  40004 
              :V9  40005 :V10 40005 :V11 40005 :V12 40005
              :V17 40007 :V18 40007 :V19 40007 :V20 40007}
        opc  {:open 1    :close 0}
        kw-should  (keyword (:should input))
        kw-v       (keyword (:valve  input))
        blks       [(:stateblock1 input)
                    (:stateblock2 input)
                    (:stateblock3 input)
                    (:stateblock4 input)]
        val-oc     (kw-should opc)
        v-blk      (nth blks (kw-v blk))
        v-pos      (kw-v vpos)
        new-adr    (kw-v adr)
        new-state  (assoc v-blk v-pos val-oc)]
    (assoc
     (dissoc task
             :PreScript
             :PreInput)
     :Address new-adr
     :Value new-state)))

(defn resolve-pre-script
  "Checks if the task has a `:PreScript` (name of the script to run)
  and an `:Input` key. If not `task` is returned."

  [task state-key]
  (if-let [script (:PreScript task)]
    (if-let [input (:PreInput task)]
      (condp = script
        "set_valve_pos" (do
                          (timbre/debug "found prescript: " input)
                          (set-valve-pos task input state-key))
        (do
          (timbre/error "script with name: " script " not implemented")
          (timbre/error "will set state: " state-key " to error")
          (st/set-val! state-key "error")))
      task)
    task))

(defn modbus!
  "Param is called `pre-task` because some tasks come with a
  `:PreScript` which has to be executed in order to complete
  the task (sometimes the `:Value` is computed be the
  `:PreScript`).
  
  ```clojure
  
   (modbus! ((meta (var modbus!)) :example-task)
            ((meta (var modbus!)) :example-state-key))
  ```"
  {:example-state-key "example"
   :example-task 
   {
    :TaskName "VS_NEW_SE3-set_valve_pos"
    :Comment "Setzt die Ventilposition."
    :Action "MODBUS"
    :StructKey "example@container@0@definition@0@1"
    :StateKey "example@container@0@state@0@1"
    :MpName "modbus"
    :Host "172.30.56.46"
    :FunctionCode "writeSingleRegister"
    :PreInput
    {
     :should "open"
     :valve "V1"
     :stateblock1 [1 0 1 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 1 0]
     :stateblock2 [0 0 1 0 0 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0]
     :stateblock3 [0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 1 1 1 0 0 0 0 0]
     :stateblock4 [0 0 0 0 0 0 0 0 1 1 1 0 0 0 0 0 0 0 0 0 0 0 0 0]
     }
    :PreScript "set_valve_pos"
    }
   }
  [pre-task state-key]
  (st/set-val! state-key "working")
  (Thread/sleep mtp)
  (let [task (resolve-pre-script pre-task state-key)]
    (println task)
    (st/set-val! state-key "executed")))
