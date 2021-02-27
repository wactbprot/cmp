(ns cmp.resp
  ^{:author "wactbprot"
    :doc "Catches responses and dispatchs."}
  (:require [cheshire.core           :as che]
            [cmp.config              :as config]
            [cmp.exchange            :as exch]
            [cmp.doc                 :as doc]
            [cmp.lt-mem              :as lt]
            [cmp.st-mem              :as st]
            [cmp.st-utils            :as stu]
            [cmp.utils               :as u]
            [com.brunobonacci.mulog  :as mu]))



(defn retry!
  [state-key]
  (let [retry-key (stu/key->retry-key state-key)
        n         (u/ensure-int (st/key->val retry-key))
        n-max     (config/max-retry (config/config))]
    (if (>= n n-max)
      (do
        (mu/log ::retry! :error "reached max-retry"  :key state-key)
        (st/set-val! retry-key 0)
        {:error "max retry"})
      (do
        (mu/log ::retry! :message (str "retry " n "/" n-max) :key state-key)
        (st/set-val! retry-key (inc n))
        {:ok "retry"}))))

        

(defn dispatch
  "Dispatches responds from outer space. Expected responses are:

  * Result ... data will be stored in long term memory 
  * ToExchange ... data goes to exchange interface
  * ids ... renew the active documents 
  * error ... state will be set to error, processing stops 
  
  It's maybe a good idea to save the respons body to a key associated
  to the state key (done).
  
  TODO: detect missing values that should be measured again.
  Solution: Missing or wrong values are detected at postscripts which
  leads to `:Retry true`. "
  [body task state-key]
  (mu/log ::dispatch :message "try to write response" :key state-key )
  (st/set-val! (stu/key->response-key state-key) body)
  (if-let [err (:error body)]
    (st/set-state! state-key :error err)
    (let [retry    (:Retry      body)
          to-exch  (:ToExchange body)
          results  (:Result     body)
          ids      (:ids        body)
          doc-path (:DocPath    task)
          mp-id    (:MpName     task)]  
      (if retry
        (let [res-retry (retry! state-key)]
          (cond
            (:error res-retry) (st/set-state! state-key :error)
            (:ok    res-retry) (st/set-state! state-key :ready)))
        (let [res-ids   (doc/renew! mp-id ids)
              res-exch  (exch/to!   mp-id to-exch)
              res-doc   (doc/store! mp-id results doc-path)]
          (cond
            (:error res-exch) (st/set-state! state-key :error "error at exchange")
            (:error res-doc)  (st/set-state! state-key :error "error at document")
            (and
             (:ok res-ids)
             (:ok res-exch)     
             (:ok res-doc))  (st/set-state! state-key (if (exch/stop-if task) :executed :ready) "exch and doc ok")
            :unexpected      (st/set-state! state-key :error "unexpected response")))))))

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
