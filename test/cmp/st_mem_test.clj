(ns cmp.st-mem-test
  (:require [clojure.test :refer :all]
            [cmp.st-mem :refer :all]))

(def k "test")

(deftest set-get-del-i
  (testing "string"
    (let [v "string"]
      (set-val! k v) 
      (is (= v (key->val k))
          (str "set get " v)))))
