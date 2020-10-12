(ns cmp.worker.pre-script
  ^{:author "wactbprot"
    :doc "prescripts."}
  (:require [taoensso.timbre :as timbre]))

(defn get-switch-pos
  "Get the valve switch position.

  ```javascript
  var wa = {'E1':45395, 'E2':45395, 'E3':45395, 'E4':45395,
            'E5':45397, 'E6':45397, 'E7':45397, 'E8':45397,
            'E9':45399, 'E10':45399, 'E11':45399, 'E12':45399,
            'E13':45401, 'E14':45401, 'E15':45401, 'E16':45401,
            'E17':45403, 'E18':45403, 'E19':45403, 'E20':45403
  };
  var ad = wa['%switch'];
  var ret = {'Address':ad}; ret;],
  ```"
  [task]
  (let [input (:PreInput task)
        adr {:E1  45395 :E2  45395 :E3  45395 :E4  45395
             :E5  45397 :E6  45397 :E7  45397 :E8  45397
             :E9  45399 :E10 45399 :E11 45399 :E12 45399
             :E13 45401 :E14 45401 :E15 45401 :E16 45401
             :E17 45403 :E18 45403 :E19 45403 :E20 45403}
        kw   (keyword (:switch  input))]
    (assoc
     (dissoc task
             :PreScript
             :PreInput)
     :Address (kw adr))))


(defn get-valve-pos
  "Get the valve position.

  ```javascript
    var wa ={
    'V1':45407, 'V2':45407, 'V3':45407, 'V4':45407,,
    'V5':45409, 'V6':45409, 'V7':45409, 'V8':45409,,
    'V9':45411,'V10':45411,'V11':45411,'V12':45411,,
    'V13':45413,'V14':45413,'V15':45413,'V16':45413,,
    'V17':45415,'V18':45415,'V19':45415,'V20':45415};,
    var ad = wa['%valve'];,
    var ret ={'Address':ad}; ret;
  ```"
  [task]
  (let [input (:PreInput task)
        adr {:V1  45407 :V2  45407 :V3  45407 :V4  45407
             :V5  45409 :V6  45409 :V7  45409 :V8  45409 
             :V9  45411 :V10 45411 :V11 45411 :V12 45411
             :V13 45413 :V14 45413 :V15 45413 :V16 45413
             :V17 45415 :V18 45415 :V19 45415 :V20 45415}
        kw-v (keyword (:valve  input))]
    (assoc
     (dissoc task
             :PreScript
             :PreInput)
     :Address (kw-v adr))))


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
  [task]
 
  (let [{should :should 
         valve  :valve
         blk1   :stateblock1
         blk2   :stateblock2
         blk3   :stateblock3
         blk4   :stateblock4
         blk5   :stateblock5} (:PreInput task)]
    (let [blk  {:V1  0     :V2  0     :V3  0     :V4  0 
                :V5  1     :V6  1     :V7  1     :V8  1 
                :V9  2     :V10 2     :V11 2     :V12 2
                :V13 3     :V14 3     :V15 3     :V16 3
                :V17 4     :V18 4     :V19 4     :V20 4}
          vpos {:V1  0     :V2  2     :V3  4     :V4  6 
                :V5  0     :V6  2     :V7  4     :V8  6 
                :V9  4     :V10 2     :V11 0     :V12 6
                :V13 0     :V14 2     :V15 4     :V16 6
                :V17 0     :V18 1     :V19 2     :V20 3}
          adr  {:V1  40003 :V2  40003 :V3  40003 :V4  40003
                :V5  40004 :V6  40004 :V7  40004 :V8  40004 
                :V9  40005 :V10 40005 :V11 40005 :V12 40005
                :V13 40006 :V14 40006 :V15 40006 :V16 40006
                :V17 40007 :V18 40007 :V19 40007 :V20 40007}
          valve-kw   (keyword valve)
          v-pos      (valve-kw vpos)
          new-adr    (valve-kw adr)
          blks       [blk1 blk2 blk3 blk4 blk5]
          val-oc     ((keyword should) {:open 1 :close 0})
          v-blk      (nth blks (valve-kw blk))
          new-state  (assoc v-blk v-pos val-oc)]
      (assoc
       (dissoc task
               :PreScript
               :PreInput)
       :Address new-adr
       :Value new-state))))

