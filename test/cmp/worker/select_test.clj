(ns cmp.worker.select-test
  (:require [clojure.test :refer :all]
            [cmp.worker.select :refer :all]))

(deftest conds-match?-i
  (testing "Checks condition vector"
    (is (true? (conds-match? [{:cond-match true}
                              {:cond-match true}]))
        "returns true")
    (is (false? (conds-match? [{:cond-match false}
                              {:cond-match true}]))
        "returns false")))

(deftest cond-match?-i
  (testing "Check condition match"
    (is (true? (cond-match? 1 "eq" 1))
        "return true")
    (is (false? (cond-match? 0 "eq" 1))
        "return false")
    (is (true? (cond-match? "1" "eq" "1"))
        "return true")
    (is (true? (cond-match? 0 "lt" 1))
        "return true")
    (is (false? (cond-match? 1 "lt" 1))
        "return false")
    (is (true? (cond-match? 1 :eq 1))
        "return true")
    (is (true? (cond-match? 1 :gt 0))
        "return true")))
