(ns cmp.st-mem-test
  (:require [clojure.test :refer :all]
            [cmp.utils :as u]
            [cmp.st-mem :refer :all]))

(def k "test")

(deftest set-get-del-i
  (testing "string"
    (let [v "string"]
      (set-val! k v) 
      (is (= v (key->val k))
          (str "set get " v))
      (del-key! k)
      (is (nil? (key->val k))
          (str "set get " v)))))

(deftest set-get-del-ii
  (testing "bool"
    (let [v true]
      (set-val! k v) 
      (is (= v (key->val k))
          (str "set get " v))
      (del-key! k)
      (is (nil? (key->val k))
          (str "set get " v)))))

(deftest set-get-del-iii
  (testing "number"
    (let [v 123456789]
      (set-val! k v) 
      (is (= v (key->val k))
          (str "set get " v))
      (del-key! k)
      (is (nil? (key->val k))
          (str "set get " v)))))

(deftest set-get-del-iv
  (testing "vec"
    (let [v [1]]
      (set-val! k v) 
      (is (= v (key->val k))
          (str "set get " v))
      (del-key! k)
      (is (nil? (key->val k))
          (str "set get " v)))))

(deftest set-get-del-v
  (testing "vec"
    (let [v {:a 1}]
      (set-val! k v) 
      (is (= v (key->val k))
          (str "set get " v))
      (del-key! k)
      (is (nil? (key->val k))
          (str "set get " v)))))

(deftest set-get-del-vi
  (testing "vec"
    (let [v '(1)]
      (set-val! k v) 
      (is (= v (key->val k))
          (str "set get " v))
      (del-key! k)
      (is (nil? (key->val k))
          (str "set get " v)))))


(deftest set-get-del-vi
  (testing "vec"
    (let [v '(1)]
      (set-same-val! [k] v) 
      (is (= v (key->val k))
          (str "set get " v))
      (del-keys! [k])
      (is (nil? (key->val k))
          (str "set get " v)))))

(deftest clear-i
  (testing "vec"
    (let [v  1
          kk (u/vec->key [k k])]
      (set-val! kk v) 
      (is (= v (key->val kk))
          (str "set get " v))
      (clear! k)
      (is (nil? (key->val kk))
          (str "set get " v)))))
