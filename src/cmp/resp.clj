(ns cmp.resp
  ^{:author "wactbprot"
    :doc "Catches responses and dispatchs."}
  (:require [cheshire.core           :as che]
            [cmp.exchange            :as exch]
            [cmp.doc                 :as doc]
            [cmp.key-utils           :as ku]
            [cmp.lt-mem              :as lt]
            [cmp.st-mem              :as st]
            [cmp.utils               :as u]
            [com.brunobonacci.mulog  :as mu]))

(defn dispatch
  "Dispatches responds from outer space. Expected responses are:

  * Result
  * ToExchange
  * ids
  * error
  
  It's maybe a good idea to save the respons body to a key associated
  to the state key (done).
  "
  [body task state-key]
  (mu/log ::dispatch :message "try to write response" :key state-key )
  (st/set-val! (ku/key->response-key state-key) body)
  (if-let [err (:error body)]
    (st/set-state! state-key :error err)
    (let [to-exch  (:ToExchange body)
          results  (:Result     body)
          ids      (:ids        body)
          doc-path (:DocPath    task)
          mp-id    (:MpName     task)]
      (let [res-ids   (doc/renew  mp-id ids)
            res-exch  (exch/to!   mp-id to-exch)
            res-doc   (doc/store! mp-id results doc-path)]
        (cond
          (:error res-exch) (st/set-state! state-key :error "error at exchange")
          (:error res-doc)  (st/set-state! state-key :error "error at document")
          (and
           (:ok res-ids)
           (:ok res-exch)     
           (:ok res-doc))  (st/set-state! state-key (if (exch/stop-if task) :executed :ready) "exch and doc ok")
          :unexpected      (st/set-state! state-key :error "unexpected response"))))))

;;------------------------------
;; check
;;------------------------------
(defn check
  "Checks a response from outer space.  Lookes at the status, parses the
  body and dispathes."
  [res task state-key]
  (if-let [status (:status res)]
    (if-let [body (che/decode (:body res) true)]
      (if (< status 400) 
        (dispatch body task state-key) 
        (mu/log ::check :error "request for failed" :key  state-key ))            
      (mu/log ::check :error "body can not be parsed" :key state-key))
    (mu/log ::check :error "no status in header" :key state-key)))
