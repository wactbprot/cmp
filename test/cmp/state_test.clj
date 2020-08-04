(ns cmp.state-test
  (:require [clojure.test :refer :all]
            [cmp.state :refer :all]
            [cmp.utils :as u]))

(deftest state-map->definition-key-i
  (testing "key transform. (i)"
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
  (testing "a key transform. (i)"
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
        "second par step (error is filtered by choose-next)")
    (is (nil?
         (next-map
          [{:seq-idx 0, :par-idx 0, :state :executed}
           {:seq-idx 0, :par-idx 1, :state :working}
           {:seq-idx 1, :par-idx 0, :state :ready}
           {:seq-idx 2, :par-idx 0, :state :ready}
           {:seq-idx 3, :par-idx 0, :state :ready}
           {:seq-idx 3, :par-idx 1, :state :ready}]))
        "nothing should be done on error (nil is returned)")
    (is (= {:seq-idx 1, :par-idx 0, :state :ready}
           (next-map
            [{:seq-idx 0, :par-idx 0, :state :executed}
             {:seq-idx 0, :par-idx 1, :state :executed}
             {:seq-idx 1, :par-idx 0, :state :ready}
             {:seq-idx 2, :par-idx 0, :state :ready}
             {:seq-idx 3, :par-idx 0, :state :ready}
             {:seq-idx 3, :par-idx 1, :state :ready}]))
        "nothing should be done on error (nil is returned)")
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




(deftest choose-next-i
  (testing "choose next thing to do(i)"
    (is (= :error
           (:what (choose-next [{:seq-idx 0, :par-idx 0, :state :error}])))
        "error.")
    (is (= :error
           (:what (choose-next [{:seq-idx 0, :par-idx 0, :state :error}
                                {:seq-idx 0, :par-idx 1, :state :working}
                                {:seq-idx 1, :par-idx 0, :state :ready}
                                {:seq-idx 2, :par-idx 0, :state :ready}])))
        "error first position.")
    (is (= :error
           (:what (choose-next [{:seq-idx 0, :par-idx 0, :state :working}
                                {:seq-idx 0, :par-idx 1, :state :error}
                                {:seq-idx 1, :par-idx 0, :state :ready}
                                {:seq-idx 2, :par-idx 0, :state :ready}])))
        "error second position.")
    (is (= :error
           (:what (choose-next [{:seq-idx 0, :par-idx 0, :state :working}
                                {:seq-idx 0, :par-idx 1, :state :working}
                                {:seq-idx 1, :par-idx 0, :state :ready}
                                {:seq-idx 2, :par-idx 0, :state :error}])))
        "error last position.")
    (is (= :all-exec
           (:what (choose-next [{:seq-idx 0, :par-idx 0, :state :executed}])))
        "all done.")
    (is (= :nop
           (:what (choose-next [{:seq-idx 0, :par-idx 0, :state :working}])))
        "nop.")
    (is (= :work
           (:what (choose-next [{:seq-idx 0, :par-idx 0, :state :ready}])))
        "work.")))
