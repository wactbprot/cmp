(ns cmp.resp
  ^{:author "wactbprot"
    :doc "Catches responses and dispatchs."}
  (:require [clojure.core.async :as a]
            [cmp.config :as cfg]
            [cmp.exchange :as exch]
            [cmp.doc :as doc]
            [cmp.lt-mem :as lt]
            [cmp.st-mem :as st]
            [cmp.utils :as u]
            [taoensso.timbre :as log]))

(def mtp (cfg/min-task-period (cfg/config)))

(defn dispatch
  "Dispatches responds from outer space.
  Expected responses are:

  * Result
  * ToExchange
  * error

  It's maybe a good idea to save the
  respons body to a key associated to
  the state key (done).
  "
  [body task state-key]
  (if-let [err (:error body)]
    (throw (Exception. (str "response: " body " at " state-key)))
    (let [resp-key (st/state-key->response-key state-key)
          to-exch  (:ToExchange body)
          results  (:Result body) 
          doc-path (:DocPath task)
          mp-id    (:MpName task)]
      (log/debug "write response body to: " resp-key)
      (st/set-val! resp-key body)
      (let [res-exch  (exch/to! mp-id to-exch)
            res-doc   (doc/store! mp-id results doc-path)]
        (cond
          (:error res-exch) (do
                              (Thread/sleep mtp)
                              (st/set-val! state-key "error")
                              (throw (Exception. (str "error on exch/to! at: " state-key))))
          (:error res-doc)  (do
                              (Thread/sleep mtp)
                              (st/set-val! state-key "error")
                              (throw (Exception. (str "error on doc/store! at: " state-key))))
          (and
           (:ok res-exch)     
           (:ok res-doc))   (do
                              (Thread/sleep mtp)
                              (st/set-val! state-key "executed")
                              (log/info "response handeled for: " state-key))
          :unexpected       (do
                              (Thread/sleep mtp)
                              (st/set-val! state-key "error")
                              (throw (Exception. (str "unexpected behaviour at: " state-key )))))))))

;;------------------------------
;; check
;;------------------------------
(defn check
  "Checks a response from outer space.
  Lookes at the status, parses the body and dispathes."
  [res task state-key]
  (if-let [status (:status res)]
    (if-let [body (u/val->clj (:body res))]
      (if (< status 400) 
        (dispatch body task state-key) 
        (throw (Exception. (str "request for: " state-key
                                " failed with status: " status))))            
       (throw (Exception. (str "body can not be parsed for: "
                              state-key))))
    (throw (Exception. (str "no status in header for: "
                            state-key)))))
