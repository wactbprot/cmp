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

(deftest replace-key-at-level-i
  (testing "Substitude parts in key (i)"
    (is (= "aaa" (replace-key-at-level 0 "kkk" "aaa")) 
        "basic functionality l 0")
    (is (= "kkk@aaa" (replace-key-at-level 1 "kkk@kkk" "aaa")) 
        "basic functionality l 1")
    (is (nil? (replace-key-at-level 10 "kkk@kkk" "aaa")) 
        "return nil if l > count split of key")))

(deftest doc->safe-doc-i
  (testing "Substitude @ by % (i)"
    (is (= {:a "%kk"} (doc->safe-doc {:a "@kk"})) 
        "basic functionality")))

(deftest short-string-i
  (testing "cuts a string (i)"
    (is (= "aaa" (short-string "aaa")) 
        "cuts a short string")
    (is (nil? (short-string nil)) 
        "dont crash on nil")
    (is (= "" (short-string "")) 
        "cuts an empty string")
    (is (= "..." (short-string "a" 0)) 
        "adds dots if shorten")))