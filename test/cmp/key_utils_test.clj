(ns cmp.key-utils-test
  (:require [clojure.test  :refer :all]
            [cmp.key-utils :refer :all]
            [cmp.utils     :as u]))

(deftest info-map->definition-key-i
  (testing "key transform. (i)"
    (is (= (str "a" u/sep
                "b" u/sep
                "c" u/sep
                "definition" u/sep
                "d" u/sep
                "e")
           (info-map->definition-key {:mp-id "a"
                                      :struct  "b"
                                      :no-idx  "c"
                                      :seq-idx "d"
                                      :par-idx "e"}))
        "produced.")
    (is (nil? (info-map->definition-key ""))
        "nil .")))

(deftest info-map->ctrl-key-i
  (testing "a key transform. (i)"
    (is (= (str "a" u/sep
                "b" u/sep
                "c" u/sep
                "ctrl")
           (info-map->ctrl-key {:mp-id "a"
                                :struct  "b"
                                :no-idx  "c"}))
        "produced.")
    (is (nil? (info-map->ctrl-key ""))
        "nil .")))

(deftest k->state-ks-i
  (testing "a key returns (i)"
    (is (nil? (k->state-ks nil))
        "nil .")))

(deftest key->info-map-i
  (testing "generating a key map (i)"
    (is (= {:mp-id    "a"
            :struct   "b"
            :func     "d"
            :no-idx   "0"
            :seq-idx  "1"
            :par-idx  "2"}
           (key->info-map "a@b@0@d@1@2"))
        "extracts the mp-id")
    (is (= {:mp-id    "a"
            :struct   "b"
            :func     "d"
            :no-idx   "000"
            :seq-idx  "001"
            :par-idx  "002"}
           (key->info-map "a@b@000@d@001@002"))
        "extracts the mp-id")
    (is (nil? (:mp-id (key->info-map "")))
        "generates nil")
    (is (nil? (key->info-map nil))
        "returns nil")))
