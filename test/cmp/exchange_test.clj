(ns cmp.exchange-test
  (:require [clojure.test :refer :all]
            [cmp.exchange :refer :all]
            [cmp.st-utils :as stu]
            [cmp.st-mem :as st]))

(deftest stop-if-i
  (testing "StopIf"
    (st/set-val! "test@exchange@A" {:B "ok"})
    (is (= true (stop-if {:MpName "test" :StopIf "A.B"}))
        "returns true on ok")
    (is (= false (stop-if {:MpName "test" :StopIf "A.C"}))
        "returns false if not exist")
    (is (= true (stop-if {}))
        "returns false if kw not exist")))

(deftest run-if-i
  (testing "RunIf"
    (st/set-val! "test@exchange@A" {:B "ok"})
    (is (= true (run-if {:MpName "test" :RunIf "A.B"}))
        "returns true on ok")
    (is (= false (run-if {:MpName "test" :RunIf "A.C"}))
        "returns false if not exist")
    (is (= true (run-if {}))
        "returns false if kw not exist")))

(deftest to-i
  (testing "to!"
    (to! "test" {:B "aaa"})
    (is (= "aaa" (st/key->val (stu/exch-key "test" "B")))
        "stores string")))

(deftest to-ii
  (testing "to! with simple path"
    (to! "test" {:B "aaa"} "dd")
    (is (= {:B "aaa"}  (st/key->val (stu/exch-key "test" "dd")))
        "stores string under path "))
  (testing "to! with double path"
    (to! "test" {:B "aaa"} "dd.ff")
    (is (= {:ff {:B "aaa"}}  (st/key->val (stu/exch-key "test" "dd")))
        "stores string under path "))
  (testing "to! with nil path"
    (to! "test" {:B "aaa"} nil)
    (is (= "aaa"  (st/key->val (stu/exch-key "test" "B")))
        "don't crash ")))

(deftest from-i
  (testing "from! with map"
    (st/set-val! "test@exchange@C" {:D "ok"})
    (is (= {:F "ok"}  (from! "test" {:F "C.D"}))
        "gets and replaces")
    (is (nil? (from! nil {:F "C.D"}))
        "returns nil on missing mp-id")))

(deftest read-i
  (testing "read! with dot path"
    (st/set-val! "test@exchange@E.D"  "ok")
    (is (= "ok"  (read! "test" "E.D"))
        "gets ok")
    (is (nil?  (read! "test" "E.F"))
        "don't crash on nil")))

(deftest key->kw-i
  (testing "key->kw"
    (is (= :B (key->second-kw "A.B"))
        "returns kw")
    (is (nil?  (key->second-kw "A"))
        "returns kw")))
