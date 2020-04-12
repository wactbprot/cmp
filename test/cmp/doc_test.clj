(ns cmp.doc-test
  (:require [clojure.test :refer :all]
            [cmp.doc :refer :all]))

(def m-vec {:Type "a" :Unit "a" :Value [0] :SdValue [0] :N [1]})
(def m-val {:Type "b" :Unit "b" :Value 1 :SdValue 1 :N 1})
(def m-vaa {:Type "a" :Unit "b" :Value 1 :SdValue 1 :N 1})


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

(def doc1 {:Calibration {:Measurement
                         {:Values
                          {:Pressure []}}}})
(def doc2 {:Calibration {:Measurement
                         {:Values
                          {:Pressure [
                                      {:Type "a"
                                       :Unit "a"
                                       :Value [0]
                                       :SdValue [0]
                                       :N [1]}]}}}})

(def p1 "Calibration.Measurement.Values.Pressure")
(def p2 "Calibration.Measurement.Values.not-there")
(def p3 "Calibration.Measurement.AuxValues")

(def v1 [:Calibration :Measurement :Values :Pressure])
(def v2 [:Calibration :Measurement :Values :not-there])
(def v3 [:Calibration :Measurement :AuxValues])

(deftest store-result-i
  (testing "results are stored (i)"
    (is (= [m-vec] (get-in
                    (store-result doc1 m-vec p1)
                    v1)) 
        "m-vec is inserted at path.")
    (is (= [m-vec] (get-in
                    (store-result doc1 m-vec p2)
                    v2)) 
        "m-vec is inserted if path is not present.")
    (is (= [1] (:Value
                (nth
                 (get-in
                  (store-result doc1 m-val p1)
                  v1)
                 0))) 
        "Value becomes a vector and is inserted at path.")
    (is (= [1] (:Value
                (nth
                 (get-in
                  (store-result doc1 m-val p2)
                  v2)
                 0))) 
        "Value becomes a vector and is inserted at non existing path.")
    (is (= 2 (count
              (get-in
               (store-result doc2 m-val p1)
               v1))) 
        "Map is added to existing path (Type differs).")
    (is (= [0 1] (:Value
                  (nth
                   (get-in
                    (store-result doc2 m-vaa p1)
                    v1) 0))) 
        "Value is appended to existing path and structure (without vectors).")
    (is (= "b" (:Unit
                  (nth
                   (get-in
                    (store-result doc2 m-vaa p1)
                    v1) 0))) 
        "Unit is updated at existing path and structure.")))

(deftest store-result-ii
  (testing "results are stored plain (ii)"
    (is (= {:OPK 1} (get-in
                    (store-result doc1 {:OPK 1} p3)
                    v3)) 
        "Map is added.")
    (is (= {:OPK 2} (get-in
                     (store-result
                      (store-result doc1 {:OPK 1} p3)
                      {:OPK 2} p3)
                    v3)) 
        "Map is updated.")
    (is (= {:OPK 1 :Gas "N2"} (get-in
                     (store-result
                      (store-result doc1 {:OPK 1} p3)
                      {:Gas "N2"} p3)
                    v3)) 
        "Map is assoced.")))

(deftest store-results-i
  (testing "results are stored(i)"
    (is (= [0 1] (:Value
                  (nth
                   (get-in
                    (store-results doc1 [m-vec m-val m-vaa] p1)
                    v1)
                   0)))
        "Values got attached if they have equal Types.")
    (is (= [1] (:Value
                  (nth
                   (get-in
                    (store-results doc1 [m-vec m-val m-vaa] p1)
                    v1)
                   1)))
        "Map is inserted and values become vextors.")))
