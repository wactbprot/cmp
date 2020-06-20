(ns cmp.worker.devhub-test
  (:require [clojure.test :refer :all]
            [cmp.worker.devhub :refer :all]))

(def task  {
            :TaskName "VS_NEW_SE3-set_valve_pos"
            :Comment "Setzt die Ventilposition."
            :Action "MODBUS"
            :StateKey "example@container@0@state@0@1"
            :MpName "devhub"
            :Host "172.30.56.46"
            :FunctionCode "writeSingleRegister"
            :PreInput
            {
             :should "open"
             :valve "V1"
             :stateblock1
             [1 0 1 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 1 0]
             :stateblock2
             [0 0 1 0 0 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0]
             :stateblock3
             [0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 1 1 1 0 0 0 0 0]
             :stateblock4
             [0 0 0 0 0 0 0 0 1 1 1 0 0 0 0 0 0 0 0 0 0 0 0 0]
             }
            :PreScript "set_valve_pos"
            })
 
(deftest resolve-pre-script-test-i
  (testing "Returns task"
    (is (map? (resolve-pre-script task))
        "Got the right type.")))