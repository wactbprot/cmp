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

(defn state-key->response-key
  "Turns the given `state-key` into a
  `response-key`.

  ```clojure
  (state-key->response-key \"devs@container@0@state@0@0\")
  ;; devs@container@0@response@0@0
  ```
  "
  [state-key]
  (u/replace-key-at-level 3 state-key "response"))


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
    (a/>!! excep/ch (throw (str "response: " body " at " state-key)))
    (let [resp-key (state-key->response-key state-key)
          to-exch  (:ToExchange body)
          results  (:Result body) 
          doc-path (:DocPath task)
          mp-id    (:MpName task)]
      (timbre/debug "write response body to: " resp-key)
      (st/set-val! resp-key body)
      (let [res-exch  (exch/to! mp-id to-exch)
            res-doc   (doc/store! mp-id results doc-path)]
        (prn res-exch)
        (prn res-doc)
        
        (cond
          (:error res-exch) (do
                              (st/set-val! state-key "error")
                              (a/>!! excep/ch (throw (str "error on exch/to! at: " state-key))))
          (:error res-doc)  (do
                              (st/set-val! state-key "error")
                              (a/>!! excep/ch (throw (str "error on doc/store! at: " state-key))))
          (and
           (:ok res-exch)     
           (:ok res-doc))   (do
                              (st/set-val! state-key "executed")
                              (timbre/info "response handeled for: " state-key))
          :default          (do
                              (st/set-val! state-key "error")
                              (a/>!! excep/ch (throw (str "unexpected behaviour at: " state-key)))))))))

;;------------------------------
;; ctrl channel buffers
;; 10 response processings
;;------------------------------
(def ctrl-chan (a/chan (a/buffer 10)))

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
            :default (a/>! excep/ch
                           (throw (str "request for: "
                                       state-key
                                       " failed with status: "
                                       status))))
          (a/>! excep/ch (throw (str "body can not be parsed for: "
                                      state-key))))
        (a/>! excep/ch (throw (str "no status in header for: "
                                    state-key))))
      (catch Exception e
        (timbre/error "catch error at channel "
                      state-key)
        (a/>! excep/ch e))))
  (recur))
