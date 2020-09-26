(ns cmp.st-mem-test
  (:require [clojure.test :refer :all]
            [cmp.st-mem :refer :all]))

(deftest key->key-map-i
  (testing "generating a key map (i)"
    (is (= {:mp-id    "a"
            :struct   "b"
            :func     "d"
            :no-idx   "0"
            :seq-idx  "1"
            :par-idx  "2"}
           (key->key-map "a@b@0@d@1@2"))
        "extracts the mp-id")
    (is (= {:mp-id    "a"
            :struct   "b"
            :func     "d"
            :no-idx   "000"
            :seq-idx  "001"
            :par-idx  "002"}
           (key->key-map "a@b@000@d@001@002"))
        "extracts the mp-id")
    (is (nil? (:mp-id (key->key-map "")))
        "generates nil")
    (is (nil? (key->key-map nil))
        "returns nil")))
