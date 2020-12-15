(ns cmp.utils-test
  (:require [clojure.test :refer :all]
            [cmp.utils :refer :all]))

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

(deftest safe-doc-i
  (testing "cuts a string (i)"
    (is (= {:a "(@101,102)"} (doc->safe-doc {:a "(@101,102)"})) 
        "conserves @ infront of numbers")
    (is (= {:a "foo (@101,102) bar"} (doc->safe-doc {:a "foo (@101,102) bar"})) 
        "conserves @ infront of numbers")
    (is (= {:a "%foo (@101,102)%bar"} (doc->safe-doc {:a "@foo (@101,102)%bar"})) 
        "conserves @ infront of numbers")))
