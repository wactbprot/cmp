(ns cmp.doc-test
  (:require [clojure.test :refer :all]
            [cmp.doc :refer :all]))

(def m1 {:Type "a" :Unit "a" :Value [0] :SdValue [0] :N [1]})
(def m2 {:Type "b" :Unit "b" :Value 1 :SdValue 1 :N 1})

(deftest append-and-replace-test-i
  (testing "append and replace (i)"
    (is (= "b" (:Type (append-and-replace m1  m2)))
        "Got the right type.")
    (is (= "b" (:Unit (append-and-replace m1  m2)))
        "Got the right unit.")
    (is (= [0 1] (:Value (append-and-replace m1  m2)))
        "Append the value.")
    (is (= [0 1] (:SdValue (append-and-replace m1  m2)))
        "Append the sdvalue.")
    (is (= [1 1] (:N (append-and-replace m1  m2)))
        "Append the n.")))

(deftest append-and-replace-test-ii
  (testing "append and replace (ii)"
    (is (= "a" (:Type (append-and-replace m1  {})))
        "Type remains unchanged.")
    (is (= "a" (:Unit (append-and-replace m1  {})))
        "Unit remains unchanged.")
    (is (= [0] (:Value (append-and-replace m1  {})))
        "Value remains unchanged.")
    (is (= [0] (:SdValue (append-and-replace m1  {})))
        "Sdvalue remains unchanged.")
    (is (= [1] (:N (append-and-replace m1  {})))
        "N remains unchanged.")))

(deftest append-and-replace-test-iii
  (testing "append and replace (iii)"
    (is (= "b" (:Type (append-and-replace {} m2)))
        "Type is inserted.")
    (is (= "b" (:Unit (append-and-replace {} m2)))
        "Unit is inserted.")
    (is (= [1] (:Value (append-and-replace {} m2)))
        "Value is inserted.")
    (is (= [1] (:SdValue (append-and-replace {} m2)))
        "Sdvalue is inserted.")
    (is (= [1] (:N (append-and-replace {} m2)))
        "N is inserted.")))

(def doc1 {:Calibration {:Measurement {:Values {:Pressure []}}}})
(def path1 "Calibration.Measurement.Values.Pressure")
(def path2 "Calibration.Measurement.Values.not-there")
  
(def v1 [:Calibration :Measurement :Values :Pressure])

(deftest store-result-i
  (testing "results are stored (i)"
    (is (= [m1] (get-in (store-result doc1 m1 path1) v1))) 
    "m1 is inserted at path.")))
