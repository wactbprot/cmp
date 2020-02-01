(ns cmp.worker.modbus
  ^{:author "wactbprot"
    :doc "modbus worker."}
  (:require [taoensso.timbre :as timbre]
            [clojure.core.async :as a]
            [cmp.st-mem :as st]
            [cmp.utils :as u]))

(defn set-valve-pos-prescript
  "In order to avoid starting up a nodeserver to
  execute the following pre script this is a clojure
  translation.

  To make this working a `Input` like:

  ```javascript
  Input:{
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
  [input]
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
        opc  {:open 1 :close 0}
        ]))