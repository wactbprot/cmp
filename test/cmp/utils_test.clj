(ns cmp.utils-test
  (:require [clojure.test :refer :all]
            [cmp.utils :refer :all]))

(def m-vec {:Type "a" :Unit "a" :Value [0] :SdValue [0] :N [1]})
(def m-val {:Type "b" :Unit "b" :Value 1 :SdValue 1 :N 1})

(deftest vector-if-i
  (testing "makes vectors of vals (i)"
    (is (= [0] (:Value (vector-if m-vec :Value)))
        "Leaves [1] a vector.")
    (is (= [1] (:Value (vector-if m-val :Value)))
        "Makes 1 a vector.")
    (is (nil? (:Value (vector-if nil :Value)))
        "Don't crash on nil.")
    (is (nil? (:Value (vector-if m-vec nil)))
        "Don't crash on nil.")))

(def p1 "Calibration.Measurement.Values.Pressure")
(def v1 [:Calibration :Measurement :Values :Pressure])

(deftest path-to-keyword-i
  (testing "Path translation (i)"
    (is (= v1 (path->kw-vec p1)) 
        "Translates path to keyword vector.")))

(deftest val->vec-i
  (testing "Values are transformed to vector (i)"
    (is (nil? (val->vec nil)) 
        "nil remains nil.")
    (is (= [1] (val->vec 1)) 
        "1 becomes [1].")
    (is (= [1] (val->vec [1])) 
        "[1] remains [1].")))

