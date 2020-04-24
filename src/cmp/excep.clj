(ns cmp.excep
  ^{:author "wactbprot"
    :doc "Provides an exception channel."}
  (:require [taoensso.timbre :as timbre]
            [clojure.core.async :as a]))

;;------------------------------
;; exception channel 
;;------------------------------
(def ch (a/chan))
(a/go-loop []
  (let [e (a/<! ch)]
    (if (string? e)
      (timbre/error e)
      (timbre/error (.getMessage e))))
  (recur))
