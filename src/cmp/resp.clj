(ns cmp.resp
  ^{:author "wactbprot"
    :doc "Catches responses and dispatchs."}
  (:require [taoensso.timbre :as timbre]
            [clojure.core.async :as a]
            [cmp.st-mem :as st]
            [cmp.excep :as excep]))

(defn dispatch
  "Dispatches responds from outer space.
  Expected responses are:

  * Result
  * ToExchange
  * error
  * DocPath
  "
  [body state-key]
  (println body))

;;------------------------------
;; ctrl channel invoked by run 
;;------------------------------
(def ctrl-chan (a/chan))

;;------------------------------
;; ctrl go block 
;;------------------------------
(a/go-loop []
  (let [[res state-key] (a/<! ctrl-chan)]
    (timbre/debug "try dispatch response for: " state-key)
    (try
      (if-let [status (:status res)]
        (cond
          (< status 300) (dispatch (:body res) state-key)
          (= status 304) (dispatch (:body res) state-key)
          :default (a/>! excep/ch
                         (throw (str "request for: "
                                     state-key
                                     " failed with status: "
                                     status))))
        (a/>! excep/ch (throw (str "no status in header for: "
                                   state-key))))
      (catch Exception e
        (timbre/error "catch error at channel " state-key)
        (a/>! excep/ch e))))
  (recur))
