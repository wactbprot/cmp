(ns cmp.doc-test
  (:require [clojure.test :refer :all]
            [cmp.doc :refer :all]))

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

(deftest append-and-replace-test-i
  (testing "append and replace (i)"
    (is (= "b" (:Type (append-and-replace m-vec  m-val)))
        "Got the right type.")
    (is (= "b" (:Unit (append-and-replace m-vec  m-val)))
        "Got the right unit.")
    (is (= [0 1] (:Value (append-and-replace m-vec  m-val)))
        "Append the value.")
    (is (= [0 1] (:SdValue (append-and-replace m-vec  m-val)))
        "Append the sdvalue.")
    (is (= [1 1] (:N (append-and-replace m-vec  m-val)))
        "Append the n.")))

(deftest append-and-replace-test-ii
  (testing "append and replace (ii)"
    (is (= "a" (:Type (append-and-replace m-vec  {})))
        "Type remains unchanged.")
    (is (= "a" (:Unit (append-and-replace m-vec  {})))
        "Unit remains unchanged.")
    (is (= [0] (:Value (append-and-replace m-vec  {})))
        "Value remains unchanged.")
    (is (= [0] (:SdValue (append-and-replace m-vec  {})))
        "Sdvalue remains unchanged.")
    (is (= [1] (:N (append-and-replace m-vec  {})))
        "N remains unchanged.")))

(deftest append-and-replace-test-iii
  (testing "append and replace (iii)"
    (is (= "b" (:Type (append-and-replace {} m-val)))
        "Type is inserted.")
    (is (= "b" (:Unit (append-and-replace {} m-val)))
        "Unit is inserted.")
    (is (= [1] (:Value (append-and-replace {} m-val)))
        "Value is inserted.")
    (is (= [1] (:SdValue (append-and-replace {} m-val)))
        "Sdvalue is inserted.")
    (is (= [1] (:N (append-and-replace {} m-val)))
        "N is inserted.")))

(def doc1 {:Calibration {:Measurement {:Values {:Pressure []}}}})
(def doc2 {:Calibration {:Measurement
                         {:Values
                          {:Pressure [
                                      {:Type "a"
                                       :Unit "a"
                                       :Value [0]
                                       :SdValue [0]
                                       :N [1]}]}}}})

(def path1 "Calibration.Measurement.Values.Pressure")
(def path2 "Calibration.Measurement.Values.not-there")
  
(def v1 [:Calibration :Measurement :Values :Pressure])
(def v2 [:Calibration :Measurement :Values :not-there])

(deftest store-result-i
  (testing "results are stored (i)"
    (is (= [m-vec] (get-in
                    (store-result doc1 m-vec path1)
                    v1)) 
        "m-vec is inserted at path.")
    (is (= [m-vec] (get-in
                    (store-result doc1 m-vec path2)
                    v2)) 
        "m-vec is inserted if path is not present.")
    (is (= [1] (:Value
                (nth
                 (get-in
                  (store-result doc1 m-val path1)
                  v1)
                 0))) 
        "Value becomes a vector and is inserted at path.")
    (is (= [1] (:Value
                (nth
                 (get-in
                  (store-result doc1 m-val path2)
                  v2)
                 0))) 
        "Value becomes a vector and is inserted at non existing path.")
    (is (= 2 (count
              (get-in
               (store-result doc2 m-val path1)
               v1))) 
        "Map is added to existing path (Type differs).")
    (is (= [0 0] (:Value
                  (nth
                   (get-in
                    (store-result doc2 m-vec path1)
                    v1) 0))) 
        "Map is added to existing path (Type differs).")))
