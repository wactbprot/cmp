(ns cmp.core-test
  (:require [clojure.test :refer :all]
            [cmp.core :refer :all]
            [cmp.key-utils   :as ku]
            [cmp.st-mem      :as st]))

(start-log!)

(deftest task-refresh-clear-refresh-test
  (testing "refresh and clear tasks"
    (t-refresh)
    (is (pos? (count (st/pat->keys (ku/task-key "*")))))
    (t-clear)
    (Thread/sleep 100)
    (is (zero? (count (st/pat->keys (ku/task-key "*")))))
    (t-refresh)
    (is (pos? (count (st/pat->keys (ku/task-key "*")))))))

(deftest mpd-ref-build-clear-build-test
  (testing "clear ref-mpd"
    (m-build-ref)
    (Thread/sleep 200)
    (is (pos? (count (st/pat->keys "ref*"))))
    (m-clear "ref")
    (Thread/sleep 300)
    (prn (st/pat->keys "ref*"))
    (is (zero? (count (st/pat->keys "ref*"))))
    (m-build-ref)
    (Thread/sleep 200)
    (is (pos? (count (st/pat->keys "ref*"))))))

(deftest mpd-ref-container-0-test
  (testing "clear ref-mpd"
    (workon! "ref")
    (m-build-ref)
    (Thread/sleep 100)
    (c-run 0)
    (Thread/sleep 100)
    (is (= "run" (st/key->val (ku/cont-ctrl-key "ref" 0))))
    (Thread/sleep 3000)
    (is (= "ready" (st/key->val (ku/cont-ctrl-key "ref" 0))))))

(deftest mpd-ref-container-1-test
  (testing "clear ref-mpd"
    (workon! "ref")
    (m-build-ref)
    (Thread/sleep 100)
    (c-run 1)
    (Thread/sleep 100)
    (is (= "run" (st/key->val (ku/cont-ctrl-key "ref" 1))))
    (Thread/sleep 2000)
    (is (= "ready" (st/key->val (ku/cont-ctrl-key "ref" 1))))))

(deftest mpd-ref-container-2-test
  (testing "select ref-mpd"
    (workon! "ref")
    (m-build-ref)
    (Thread/sleep 100)
    (c-run 2)
    (Thread/sleep 100)
    (is (= "run" (st/key->val (ku/cont-ctrl-key "ref" 2))))
    (Thread/sleep 2000)
    (is (= "ready" (st/key->val (ku/cont-ctrl-key "ref" 2))))))

(deftest mpd-ref-container-3-test
  (testing "runMp ref-mpd"
    (workon! "ref")
    (m-build-ref)
    (Thread/sleep 100)
    (c-run 3)
    (Thread/sleep 100)
    (is (= "run" (st/key->val (ku/cont-ctrl-key "ref" 3))))
    (Thread/sleep 3000)
    (is (= "ready" (st/key->val (ku/cont-ctrl-key "ref" 3))))))

(stop-log!)
