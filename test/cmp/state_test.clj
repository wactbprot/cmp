(ns cmp.state-test
  (:require [clojure.test :refer :all]
            [cmp.state :refer :all]
            [cmp.utils :as u]))

(deftest state-map->definition-key-i
  (testing "a key (i)"
    (is (= (str "a" u/sep
                "b" u/sep
                "c" u/sep
                "definition" u/sep
                "d" u/sep
                "e")
           (state-map->definition-key {:mp-name "a"
                                       :struct  "b"
                                       :no-idx  "c"
                                       :seq-idx "d"
                                       :par-idx "e"}))
        "produced.")
    (is (nil? (state-map->definition-key ""))
        "nil .")))

(deftest state-map->ctrl-key-i
  (testing "a key (i)"
    (is (= (str "a" u/sep
                "b" u/sep
                "c" u/sep
                "ctrl")
           (state-map->ctrl-key {:mp-name "a"
                                 :struct  "b"
                                 :no-idx  "c"}))
        "produced.")
    (is (nil? (state-map->ctrl-key ""))
        "nil .")))


(deftest k->state-ks-i
  (testing "a key returns (i)"
    (is (nil? (k->state-ks nil))
        "nil .")))

(deftest ks->state-vec-i
  (testing " returns (i)"
    (is (nil? (ks->state-vec nil))
        "nil .")))

(deftest choose-next-i
  (testing "choose (i)"
    (is (= :error
           (:what (choose-next [{:seq-idx 0, :par-idx 0, :state :error}])))
        "error.")
    (is (= :all-exec
           (:what (choose-next [{:seq-idx 0, :par-idx 0, :state :executed}])))
        "all done.")
    (is (= :nop
           (:what (choose-next [{:seq-idx 0, :par-idx 0, :state :working}])))
        "nop.")
    (is (= :work
           (:what (choose-next [{:seq-idx 0, :par-idx 0, :state :ready}])))
           "work.")))


