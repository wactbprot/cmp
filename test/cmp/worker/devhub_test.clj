(ns cmp.worker.devhub-test
  (:require [clojure.test :refer :all]
            [cmp.worker.devhub :refer :all]))

(def task-1  {:PreInput {:should "open"
                       :valve "V1"
                       :stateblock1
                       [0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1]
                       :stateblock2
                       [0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 0]
                       :stateblock3
                       [0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 0 0]
                       :stateblock4
                       [0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 0 0 0]
                       :stateblock5
                       [0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 0 0 0 0]}
            :PreScript "set_valve_pos"})

(def task-2  {:PreInput {:should "open"
                       :valve "V17"
                       :stateblock1
                       [0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1]
                       :stateblock2
                       [0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 0]
                       :stateblock3
                       [0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 0 0]
                       :stateblock4
                       [0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 0 0 0]
                       :stateblock5
                       [0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 0 0 0 0]}
            :PreScript "set_valve_pos"})
 
(deftest resolve-pre-script-test-i
  (testing "Returns task"
    (is (map? (resolve-pre-script task-1))
        "Got the right type.")
    (is (vector? (:Value (resolve-pre-script task-1)))
        "Got the right type.")
    (is (= 1
           (nth (:Value (resolve-pre-script task-1)) 0))
        "correct bit is fliped")
    (is (= 1
           (nth (:Value (resolve-pre-script task-1)) 23))
        "correct bit is fliped")))

(deftest resolve-pre-script-test-ii
  (testing "Returns task"
    (is (= 1
           (nth (:Value (resolve-pre-script task-2)) 0))
        "correct bit is fliped")
    (is (= 1
           (nth (:Value (resolve-pre-script task-2)) 19))
        "correct bit is fliped")))
