(ns cmp.exchange-test
  (:require [clojure.test :refer :all]
            [cmp.exchange :refer :all]
            [cmp.st-mem :as st]))

(deftest stop-if-i
  (testing "StopIf"
    (st/set-val! "test@exchange@A" {:B "ok"})
    (is (= true (stop-if {:MpName "test" :StopIf "A.B"}))
        "returns true on ok")
    (is (= false (stop-if {:MpName "test" :StopIf "A.C"}))
        "returns false if not exist")))

(deftest run-if-i
  (testing "RunIf"
    (st/set-val! "test@exchange@A" {:B "ok"})
    (is (= true (run-if {:MpName "test" :RunIf "A.B"}))
        "returns true on ok")
    (is (= false (run-if {:MpName "test" :RunIf "A.C"}))
        "returns false if not exist")))

(deftest to-i
  (testing "to!"
    (to! "test" {:B "aaa"})
    (is (= "aaa" (st/key->val (st/exch-path "test" "B")))
        "stores string")))

(deftest from-i
  (testing "from!"
    (st/set-val! "test@exchange@C" {:D "ok"})
    (is (= {:F "ok"}  (from! "test" {:F "C.D"}))
        "gets and replaces")
    (is (nil? (from! nil {:F "C.D"}))
        "returns nil on missing mp-id")))

(deftest key->kw-i
  (testing "key->kw"
    (is (= :B (key->kw "A.B"))
        "returns kw")
    (is (nil?  (key->kw "A"))
        "returns kw")))
