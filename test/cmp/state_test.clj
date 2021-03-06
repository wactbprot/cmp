(ns cmp.state-test
  (:require [clojure.test :refer :all]
            [cmp.state :refer :all]
            [cmp.utils :as u]))

(deftest ks->state-vec-i
  (testing " returns (i)"
    (is (nil? (ks->state-vec nil))
        "nil .")))

(deftest k->state-ks-i
  (testing "a key returns (i)"
    (is (nil? (k->state-ks nil))
        "nil .")))

(deftest next-map-i
  (testing " returns (i)"
    (is (nil? (next-map nil))
        "nil .")
    (is (nil? (next-map {}))
        "nil .")
    (is (= {:seq-idx 0, :par-idx 0, :state :ready}
           (next-map
            [{:seq-idx 0, :par-idx 0, :state :ready}
             {:seq-idx 0, :par-idx 1, :state :ready}
             {:seq-idx 1, :par-idx 0, :state :ready}
             {:seq-idx 2, :par-idx 0, :state :ready}
             {:seq-idx 3, :par-idx 0, :state :ready}
             {:seq-idx 3, :par-idx 1, :state :ready}]))
        "starts first par step")
    (is (= {:seq-idx 0, :par-idx 1, :state :ready}
           (next-map
            [{:seq-idx 0, :par-idx 0, :state :working}
             {:seq-idx 0, :par-idx 1, :state :ready}
             {:seq-idx 1, :par-idx 0, :state :ready}
             {:seq-idx 2, :par-idx 0, :state :ready}
             {:seq-idx 3, :par-idx 0, :state :ready}
             {:seq-idx 3, :par-idx 1, :state :ready}]))
        "second par step")
    (is (nil?
         (next-map
          [{:seq-idx 0, :par-idx 0, :state :working}
           {:seq-idx 0, :par-idx 1, :state :working}
           {:seq-idx 1, :par-idx 0, :state :ready}
           {:seq-idx 2, :par-idx 0, :state :ready}
           {:seq-idx 3, :par-idx 0, :state :ready}
           {:seq-idx 3, :par-idx 1, :state :ready}]))
        "nothing to do (nil is returned)")
    (is (= {:seq-idx 0, :par-idx 1, :state :ready}
           (next-map
            [{:seq-idx 0, :par-idx 0, :state :error}
             {:seq-idx 0, :par-idx 1, :state :ready}
             {:seq-idx 1, :par-idx 0, :state :ready}
             {:seq-idx 2, :par-idx 0, :state :ready}
             {:seq-idx 3, :par-idx 0, :state :ready}
             {:seq-idx 3, :par-idx 1, :state :ready}]))
        "second par step (error is filtered by start-next)")
    (is (nil?
         (next-map
          [{:seq-idx 0, :par-idx 0, :state :executed}
           {:seq-idx 0, :par-idx 1, :state :working}
           {:seq-idx 1, :par-idx 0, :state :ready}
           {:seq-idx 2, :par-idx 0, :state :ready}
           {:seq-idx 3, :par-idx 0, :state :ready}
           {:seq-idx 3, :par-idx 1, :state :ready}]))
        "")
    (is (= {:seq-idx 1, :par-idx 0, :state :ready}
           (next-map
            [{:seq-idx 0, :par-idx 0, :state :executed}
             {:seq-idx 0, :par-idx 1, :state :executed}
             {:seq-idx 1, :par-idx 0, :state :ready}
             {:seq-idx 2, :par-idx 0, :state :ready}
             {:seq-idx 3, :par-idx 0, :state :ready}
             {:seq-idx 3, :par-idx 1, :state :ready}]))
        "")
    (is (= {:seq-idx 1, :par-idx 0, :state :ready}
           (next-map
            [{:seq-idx 0, :par-idx 0, :state :executed}
             {:seq-idx 0, :par-idx 1, :state :executed}
             {:seq-idx 1, :par-idx 0, :state :ready}
             {:seq-idx 2, :par-idx 0, :state :executed}
             {:seq-idx 3, :par-idx 0, :state :ready}
             {:seq-idx 3, :par-idx 1, :state :ready}]))
        "")
    (is (= {:seq-idx 3, :par-idx 1, :state :ready}
           (next-map
            [{:seq-idx 0, :par-idx 0, :state :executed}
             {:seq-idx 0, :par-idx 1, :state :executed}
             {:seq-idx 1, :par-idx 0, :state :executed}
             {:seq-idx 2, :par-idx 0, :state :executed}
             {:seq-idx 3, :par-idx 0, :state :working}
             {:seq-idx 3, :par-idx 1, :state :ready}]))
        "last step")
    (is (nil?
           (next-map
            [{:seq-idx 0, :par-idx 0, :state :executed}
             {:seq-idx 0, :par-idx 1, :state :executed}
             {:seq-idx 1, :par-idx 0, :state :executed}
             {:seq-idx 2, :par-idx 0, :state :executed}
             {:seq-idx 3, :par-idx 0, :state :executed}
             {:seq-idx 3, :par-idx 1, :state :executed}]))
        "last step")))

(deftest next-map-ii
  (testing " returns (ii) with string keys"
    (is (= {:seq-idx "000", :par-idx "000", :state :ready}
           (next-map
            [{:seq-idx "000", :par-idx "000", :state :ready}
             {:seq-idx "000", :par-idx "001", :state :ready}
             {:seq-idx "001", :par-idx "000", :state :ready}
             {:seq-idx "002", :par-idx "000", :state :ready}
             {:seq-idx "003", :par-idx "000", :state :ready}
             {:seq-idx "003", :par-idx "001", :state :ready}]))
        "starts first par step")
    (is (= {:seq-idx "000", :par-idx "001", :state :ready}
           (next-map
            [{:seq-idx "000", :par-idx "000", :state :working}
             {:seq-idx "000", :par-idx "001", :state :ready}
             {:seq-idx "001", :par-idx "000", :state :ready}
             {:seq-idx "002", :par-idx "000", :state :ready}
             {:seq-idx "003", :par-idx "000", :state :ready}
             {:seq-idx "003", :par-idx "001", :state :ready}]))
        "second par step")
    (is (nil?
         (next-map
          [{:seq-idx "000", :par-idx "000", :state :working}
           {:seq-idx "000", :par-idx "001", :state :working}
           {:seq-idx "001", :par-idx "000", :state :ready}
           {:seq-idx "002", :par-idx "000", :state :ready}
           {:seq-idx "003", :par-idx "000", :state :ready}
           {:seq-idx "003", :par-idx "001", :state :ready}]))
        "nothing to do (nil is returned)")
    (is (= {:seq-idx "000", :par-idx "001", :state :ready}
           (next-map
            [{:seq-idx "000", :par-idx "000", :state :error}
             {:seq-idx "000", :par-idx "001", :state :ready}
             {:seq-idx "001", :par-idx "000", :state :ready}
             {:seq-idx "002", :par-idx "000", :state :ready}
             {:seq-idx "003", :par-idx "000", :state :ready}
             {:seq-idx "003", :par-idx "001", :state :ready}]))
        "second par step (error is filtered by start-next)")
    (is (nil?
         (next-map
          [{:seq-idx "000", :par-idx "000", :state :executed}
           {:seq-idx "000", :par-idx "001", :state :working}
           {:seq-idx "001", :par-idx "000", :state :ready}
           {:seq-idx "002", :par-idx "000", :state :ready}
           {:seq-idx "003", :par-idx "000", :state :ready}
           {:seq-idx "003", :par-idx "001", :state :ready}]))
        "nothing should be done on error (nil is returned)")
    (is (= {:seq-idx "001", :par-idx "000", :state :ready}
           (next-map
            [{:seq-idx "000", :par-idx "000", :state :executed}
             {:seq-idx "000", :par-idx "001", :state :executed}
             {:seq-idx "001", :par-idx "000", :state :ready}
             {:seq-idx "002", :par-idx "000", :state :ready}
             {:seq-idx "003", :par-idx "000", :state :ready}
             {:seq-idx "003", :par-idx "001", :state :ready}]))
        "nothing should be done on error (nil is returned)")
    (is (= {:seq-idx "003", :par-idx "001", :state :ready}
           (next-map
            [{:seq-idx "000", :par-idx "000", :state :executed}
             {:seq-idx "000", :par-idx "001", :state :executed}
             {:seq-idx "001", :par-idx "000", :state :executed}
             {:seq-idx "002", :par-idx "000", :state :executed}
             {:seq-idx "003", :par-idx "000", :state :working}
             {:seq-idx "003", :par-idx "001", :state :ready}]))
        "last step")
    (is (nil?
         (next-map
          [{:seq-idx "0", :par-idx "0", :state :executed}
           {:seq-idx "0", :par-idx "1", :state :executed}
           {:seq-idx "1", :par-idx "0", :state :executed}
           {:seq-idx "2", :par-idx "0", :state :executed}
           {:seq-idx "3", :par-idx "0", :state :executed}
           {:seq-idx "3", :par-idx "1", :state :executed}]))
        "last step")
    (is (nil?
         (next-map
          [{:seq-idx "0", :par-idx "0", :state :executed}
           {:seq-idx "1", :par-idx "0", :state :working}
           {:seq-idx "2", :par-idx "0", :state :ready}
           {:seq-idx "3", :par-idx "0", :state :ready}
           {:seq-idx "4", :par-idx "0", :state :ready}]))
        "bug test")
    (is (nil?
         (next-map
          [{:seq-idx "0", :par-idx "0", :state :executed}
           {:seq-idx "0", :par-idx "1", :state :executed}
           {:seq-idx "1", :par-idx "0", :state :working}
           {:seq-idx "2", :par-idx "0", :state :ready}
           {:seq-idx "3", :par-idx "0", :state :ready}
           {:seq-idx "4", :par-idx "0", :state :ready}]))
        "bug test")))


(deftest start-next-i
  (testing "choose next thing to do(i)"
    (is (= :error
           (:what (start-next [{:seq-idx 0, :par-idx 0, :state :error}])))
        "error.")
    (is (= :error
           (:what (start-next [{:seq-idx 0, :par-idx 0, :state :error}
                                {:seq-idx 0, :par-idx 1, :state :working}
                                {:seq-idx 1, :par-idx 0, :state :ready}
                                {:seq-idx 2, :par-idx 0, :state :ready}])))
        "error first position.")
    (is (= :error
           (:what (start-next [{:seq-idx 0, :par-idx 0, :state :working}
                                {:seq-idx 0, :par-idx 1, :state :error}
                                {:seq-idx 1, :par-idx 0, :state :ready}
                                {:seq-idx 2, :par-idx 0, :state :ready}])))
        "error second position.")
    (is (= :error
           (:what (start-next [{:seq-idx 0, :par-idx 0, :state :working}
                                {:seq-idx 0, :par-idx 1, :state :working}
                                {:seq-idx 1, :par-idx 0, :state :ready}
                                {:seq-idx 2, :par-idx 0, :state :error}])))
        "error last position.")
    (is (= :all-exec
           (:what (start-next [{:seq-idx 0, :par-idx 0, :state :executed}])))
        "all done.")
    (is (= :nop
           (:what (start-next [{:seq-idx 0, :par-idx 0, :state :working}])))
        "nop.")
    (is (= :work
           (:what (start-next [{:seq-idx 0, :par-idx 0, :state :ready}])))
        "work.")))


(deftest start-next-ii
  (testing "choose next thing to do (ii) with string keys."
    (is (= :error
           (:what (start-next [{:seq-idx "000", :par-idx "000", :state :error}])))
        "error.")
    (is (= :error
           (:what (start-next [{:seq-idx "000", :par-idx "000", :state :error}
                                {:seq-idx "000", :par-idx "001", :state :working}
                                {:seq-idx "001", :par-idx "000", :state :ready}
                                {:seq-idx "002", :par-idx "000", :state :ready}])))
        "error first position.")
    (is (= :error
           (:what (start-next [{:seq-idx "000", :par-idx "000", :state :working}
                                {:seq-idx "000", :par-idx "001", :state :error}
                                {:seq-idx "001", :par-idx "000", :state :ready}
                                {:seq-idx "002", :par-idx "000", :state :ready}])))
        "error second position.")
    (is (= :error
           (:what (start-next [{:seq-idx "000", :par-idx "000", :state :working}
                                {:seq-idx "000", :par-idx "001", :state :working}
                                {:seq-idx "001", :par-idx "000", :state :ready}
                                {:seq-idx "002", :par-idx "000", :state :error}])))
        "error last position.")
    (is (= :all-exec
           (:what (start-next [{:seq-idx "000", :par-idx "000", :state :executed}])))
        "all done.")
    (is (= :nop
           (:what (start-next [{:seq-idx "000", :par-idx "000", :state :working}])))
        "nop.")
    (is (= :work
           (:what (start-next [{:seq-idx "000", :par-idx "000", :state :ready}])))
        "work.")
    (is (= :all-exec
           (:what (start-next [{:seq-idx "000", :par-idx "000", :state :executed}])))
        "all done.")
    (is (= "@@000@definition@009@000"
           (:key (start-next [{:seq-idx "000" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "001" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "002" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "003" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "004" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "005" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "006" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "007" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "008" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "009" :no-idx "000", :par-idx "000", :state :ready}
                             {:seq-idx "010" :no-idx "000", :par-idx "000", :state :ready}])))
        "work.")
    (is (= "@@000@definition@010@000"
           (:key (start-next [{:seq-idx "000" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "001" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "002" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "003" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "004" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "005" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "006" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "007" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "008" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "009" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "010" :no-idx "000", :par-idx "000", :state :ready}])))
        "work.")
    (is (= "@@000@definition@003@000"
           (:key (start-next [{:seq-idx "000" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "001" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "002" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "003" :no-idx "000", :par-idx "000", :state :ready}
                             {:seq-idx "004" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "005" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "006" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "007" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "008" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "009" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "010" :no-idx "000", :par-idx "000", :state :ready}])))
        "work.")
    (is (= "@@000@definition@003@001"
           (:key (start-next [{:seq-idx "000" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "001" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "002" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "003" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "003" :no-idx "000", :par-idx "001", :state :ready}
                             {:seq-idx "004" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "005" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "006" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "007" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "008" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "009" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "010" :no-idx "000", :par-idx "000", :state :ready}])))
        "work.")
    (is (= :nop
           (:what (start-next [{:seq-idx "000" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "001" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "002" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "003" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "003" :no-idx "000", :par-idx "001", :state :working}
                             {:seq-idx "004" :no-idx "000", :par-idx "000", :state :executed}
                             {:seq-idx "005" :no-idx "000", :par-idx "000", :state :ready}
                             {:seq-idx "006" :no-idx "000", :par-idx "000", :state :ready}
                             {:seq-idx "007" :no-idx "000", :par-idx "000", :state :ready}
                             {:seq-idx "008" :no-idx "000", :par-idx "000", :state :ready}
                             {:seq-idx "009" :no-idx "000", :par-idx "000", :state :ready}
                             {:seq-idx "010" :no-idx "000", :par-idx "000", :state :ready}])))
        "nop")))
