(ns cmp.resp
  ^{:author "wactbprot"
    :doc "Catches responses and dispatchs."}
  (:require [taoensso.timbre :as timbre]
            [clojure.core.async :as a]
            [cmp.st-mem :as st]
            [cmp.excep :as excep]))



;;------------------------------
;; ctrl channel invoked by run 
;;------------------------------
(def ctrl-chan (a/chan))

;;------------------------------
;; ctrl go block 
;;------------------------------
(a/go-loop []
  (let [[resp state-key] (a/<! ctrl-chan)]
    (try
      (timbre/debug "receive response for " state-key
                    " try to dispatch")            
      (catch Exception e
        (timbre/error "catch error at channel " state-key)
        (a/>! excep/ch e))))
  (recur))
