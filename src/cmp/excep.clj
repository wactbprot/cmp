(ns cmp.excep
  ^{:author "wactbprot"
    :doc "Provides an exception channel."}
  (:require [taoensso.timbre :as timbre]
            [clojure.core.async :as a]))

;;------------------------------
;; exception channel 
;;------------------------------
(def ch (a/chan))
(a/go
  (while true
    (let [e (a/<! ch)] 
      (timbre/error (.getMessage e)))))
