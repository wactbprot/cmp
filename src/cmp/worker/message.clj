(ns cmp.worker.message
  ^{:author "wactbprot"
    :doc "message worker."}
  (:require [taoensso.timbre :as log]
            [cmp.st-mem :as st]
            [cmp.utils :as u]
            [cmp.config :as cfg]))

(defn message!
  "Writes a message to the exchange.
  Continues if message is replaced by the string `ok`
  
  ```clojure
  (message! {:Message \"cmp?\" :MpName \"ref\" :StateKey \"ref@container@10@state@0@0\"})
  ;;
  (st/key->val \"ref@container@10@message\")
  ;; cmp?
  ```"
  [{msg :Message mp-id :MpName state-key :StateKey}]
  (st/set-state! state-key :working)
  (let [struct   (st/key->struct state-key)
        no-idx   (st/key->no-idx state-key)
        msg-key  (st/message-path mp-id struct no-idx)
        func     (st/key->func msg-key)
        level    "a"
        callback (fn [_]
                   (when (contains? u/ok-set (st/key->val msg-key))
                     (st/set-state! state-key :executed "ready callback for message worker")
                     (st/de-register! mp-id struct no-idx func level)))]
    (st/set-val! msg-key msg)
    (st/register! mp-id struct no-idx func callback level)))
