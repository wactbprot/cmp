(ns cmp.cli-test
  (:require [clojure.test :refer :all]
            [cmp.cli :refer :all]
            [cmp.config     :as config]
            [cmp.key-utils  :as ku]
            [cmp.st-mem     :as st]))

(def conf (config/config))

(start-log! conf)

(deftest task-refresh-clear-refresh-test
  (testing "refresh and clear tasks"
    (t-refresh conf)
    (is (pos? (count (st/pat->keys (ku/task-key "*")))))
    (t-clear conf)
    (Thread/sleep 100)
    (is (zero? (count (st/pat->keys (ku/task-key "*")))))
    (t-refresh conf)
    (is (pos? (count (st/pat->keys (ku/task-key "*")))))))

(deftest mpd-ref-build-clear-build-test
  (testing "clear ref-mpd"
    (m-build-ref conf)
    (Thread/sleep 500)
    (m-stop conf "ref")
    (Thread/sleep 300)
    (is (pos? (count (st/pat->keys "ref*"))))
    (st/clear! "ref")
    (Thread/sleep 200)
    (prn (st/pat->keys "ref*"))
    (is (zero? (count (st/pat->keys "ref*"))))
    (m-build-ref conf)
    (Thread/sleep 200)
    (is (pos? (count (st/pat->keys "ref*"))))))

(deftest mpd-ref-container-0-test
  (testing "clear ref-mpd"
    (m-build-ref conf)
    (Thread/sleep 100)
    (c-run conf "ref" 0)
    (Thread/sleep 100)
    (is (= "run" (st/key->val (ku/cont-ctrl-key "ref" 0))))
    (Thread/sleep 3000)
    (is (= "ready" (st/key->val (ku/cont-ctrl-key "ref" 0))))))

(deftest mpd-ref-container-1-test
  (testing "clear ref-mpd"
    (m-build-ref conf)
    (Thread/sleep 100)
    (c-run conf "ref" 1)
    (Thread/sleep 100)
    (is (= "run" (st/key->val (ku/cont-ctrl-key "ref" 1))))
    (Thread/sleep 2000)
    (is (= "ready" (st/key->val (ku/cont-ctrl-key "ref" 1))))))

(deftest mpd-ref-container-2-test
  (testing "select ref-mpd"
    (m-build-ref conf)
    (Thread/sleep 100)
    (c-run conf "ref" 2)
    (Thread/sleep 100)
    (is (= "run" (st/key->val (ku/cont-ctrl-key "ref" 2))))
    (Thread/sleep 2000)
    (is (= "ready" (st/key->val (ku/cont-ctrl-key "ref" 2))))))

(deftest mpd-ref-container-3-test
  (testing "runMp ref-mpd"
    (m-build-ref conf)
    (Thread/sleep 100)
    (c-run conf "ref" 3)
    (Thread/sleep 100)
    (is (= "run" (st/key->val (ku/cont-ctrl-key "ref" 3))))
    (Thread/sleep 3000)
    (is (= "ready" (st/key->val (ku/cont-ctrl-key "ref" 3))))))

(stop-log! conf)
