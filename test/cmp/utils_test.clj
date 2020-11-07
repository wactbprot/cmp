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