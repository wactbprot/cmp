(ns cmp.task-test
  (:require [clojure.test :refer :all]
            [cmp.task :refer :all]))

(def d {"%hour" "11",
        "%minute" "22",
        "%second" "33",
        "%year" "4444",
        "%month" "55",
        "%day" "66",
        "%time" "00"
        "%motor" 1})

(deftest outer-replace-map-i
  (testing "replace strings"
    (is (= "00"
           (:Value (outer-replace-map d {:TaskName "foo" :Value "%time"})))
        "replaced")
    (is (= "%foo"
           (:Value (outer-replace-map d {:TaskName "foo" :Value "%foo"})))
        "not replaced")
    (is (= "###00###"
           (:Value (outer-replace-map d {:TaskName "foo" :Value "###%time###"})))
        "replaced if not isolated")
    (is (= "   00   "
           (:Value (outer-replace-map d {:TaskName "foo" :Value "   %time   "})))
        "whitespaces kept")
    (is (= "00\n"
           (:Value (outer-replace-map d {:TaskName "foo" :Value "%time\n"})))
        "ctrl-char kept after")
    (is (= "00\r"
           (:Value (outer-replace-map d {:TaskName "foo" :Value "%time\r"})))
        "ctrl-char kept after")
    (is (= "\r\t\n00\r\t\n"
           (:Value (outer-replace-map d {:TaskName "foo" :Value "\r\t\n%time\r\t\n"})))
        "ctrl-char kept after")
    (is (= "[00]"
           (:Value (outer-replace-map d {:TaskName "foo" :Value "[%time]"})))
        "braces kept")
    (is (= "([{00}])"
           (:Value (outer-replace-map d {:TaskName "foo" :Value "([{%time}])"})))
        "braces kept")
    (is (= "%00%"
           (:Value (outer-replace-map d {:TaskName "foo" :Value "%%time%"})))
        "% kept")))

(deftest outer-replace-map-ii
  (testing "nested replace strings"
    (is (= "'Servo_1_Pos.Value':_x,"
           (get
            (:PostProcessing
             (outer-replace-map d {:TaskName "foo"
                                   :PostProcessing ["ToExchange={"
                                                    "'Servo_%motor_Pos.Value':_x,"
                                                    "'Servo_%motor_Pos.Unit':'step'"
                                                    "};"]}))
            1))
        "replaced")
    (is (= "%00%"
           (:Foo (:Value (outer-replace-map d {:TaskName "foo"
                                               :Value {:Foo "%%time%"}}))))
        "nested % kept")))
