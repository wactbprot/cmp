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
    (m-build-edn)
    (Thread/sleep 100)
    (is (pos? (count (st/pat->keys "ref*"))))
    (m-clear "ref")
    (Thread/sleep 100)
    (is (zero? (count (st/pat->keys "ref*"))))
    (m-build-edn)
    (Thread/sleep 100)
    (is (pos? (count (st/pat->keys "ref*"))))))

(deftest mpd-ref-container-0-test
  (testing "clear ref-mpd"
    (workon! "ref")
    (m-build-edn)
    (Thread/sleep 100)
    (m-start)
    (Thread/sleep 100)
    (c-run 0)
    (is (= "run" (st/key->val (ku/cont-ctrl-key "ref" 0))))
    (Thread/sleep 5000)
    (is (= "ready" (st/key->val (ku/cont-ctrl-key "ref" 0))))))

(stop-log!)
