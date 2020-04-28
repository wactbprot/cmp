(ns cmp.resp-test
  (:require [clojure.test :refer :all]
            [cmp.resp :refer :all]))


(def body {:t_start 1587826583469,
           :t_stop 1587826592583,
           :Result [{:Type "dkmppc4",
                     :Value 24.404423321,
                     :Unit "C",
                     :SdValue 0.0010055135454,
                     :N 10}]})

(def task {:TaskName "DKM_PPC4_DMM-read_temp"
           :Port "5025" 
           :Comment "VXI-Kommunikation:"
           :StructKey nil
           :StateKey nil
           :Fallback {:Result [{:Type "dkmppc4" :Unit "C" :Value nil :SdValue nil :N nil}]},
           :Action "TCP",
           :Value "dkm()\n"
           :Host "e75496"
           :MpName nil
           :DocPath "Calibration.Measurement.Values.Temperature"
           :PostProcessing  ["var _mv=parseFloat(_x.split(',')[0]);"
                             "var _sd=parseFloat(_x.split(',')[1]);"
                             "var _n=parseFloat(_x.split(',')[2]);"
                             "Result=[_.vlRes('dkmppc4',_mv,'C','',_sd, _n)];"]})
(comment
  (deftest dispatch-i
  (testing "dispatching (i)"
    (is (= "b" (:Type (append-and-replace m-vec  m-val)))
        "Got the right type."))))
