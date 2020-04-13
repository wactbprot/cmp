(ns cmp.resp
  ^{:author "wactbprot"
    :doc "Catches responses and dispatchs."}
  (:require [clojure.core.async :as a]
            [cmp.exchange :as exch]
            [cmp.excep :as excep]
            [cmp.doc :as doc]
            [cmp.lt-mem :as lt]
            [cmp.st-mem :as st]
            [cmp.utils :as u]
            [taoensso.timbre :as timbre]))

(defn dispatch
  "Dispatches responds from outer space.
  Expected responses are:

  * Result
  * ToExchange
  * error
  "
  [body task state-key]
  (if-let [err (:error body)]
    (a/>!! excep/ch (throw (str "respons: " body " at " state-key)))
    (let [to-exch  (:ToExchange body)
          results  (:Result body) 
          path     (:DocPath body)
          mp-id    (:MpName task)]
      (exch/to! mp-id to-exch)
      (if (and (string? path) (vector? results))
        (doc/store-results {:place-holder "foo"} results path)
        ;; here: is Result translated to a vector?
        ))))



;;------------------------------
;; ctrl channel invoked by run 
;;------------------------------
(def ctrl-chan (a/chan))

;;------------------------------
;; ctrl go block 
;;------------------------------
(a/go-loop []
  (let [[res task state-key] (a/<! ctrl-chan)]
    (timbre/debug "try dispatch response for: " state-key)
    (try
      (if-let [status (:status res)]
        (if-let [body (u/val->clj  (:body res))]
          (cond
            (< status 300) (dispatch body task state-key)
            (= status 304) (dispatch body task state-key)
            :default (a/>!! excep/ch
                           (throw (str "request for: "
                                       state-key
                                       " failed with status: "
                                       status))))
          (a/>!! excep/ch (throw (str "response body can not be parsed for: "
                                     state-key))))
        (a/>!! excep/ch (throw (str "no status in header for: "
                                   state-key))))
      (catch Exception e
        (timbre/error "catch error at channel " state-key)
        (a/>!! excep/ch e))))
  (recur))
