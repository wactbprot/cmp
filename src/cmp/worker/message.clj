(ns cmp.worker.message
  ^{:author "wactbprot"
    :doc "message worker."}
  (:require [cmp.config              :as cfg]
            [com.brunobonacci.mulog  :as mu]
            [cmp.st-mem              :as st]
            [cmp.st-utils            :as stu]
            [cmp.utils               :as u]))

(defn message!
  "Writes a `:Message` to the exchange interface. Continues if message is replaced by
  something in the [[u/ok-set]].
  
  ```clojure
  (message! {:Message \"cmp?\" :MpName \"ref\" :StateKey \"ref@container@10@state@0@0\"})
  ;;
  (st/key->val \"ref@container@10@message\")
  ;; cmp?
    ```"
  [{msg :Message mp-id :MpName state-key :StateKey}]
  (st/set-state! state-key :working)
  (let [struct   (stu/key->struct state-key)
        no-idx   (stu/key->no-idx state-key)
        msg-key  (stu/key->message-key state-key)
        func     (stu/key->func msg-key)
        level    "a"
        callback (fn [_]
                   (when (contains? u/ok-set (st/key->val msg-key))
                     (st/set-state! state-key :executed "ready callback for message worker")
                     (st/de-register! mp-id struct no-idx func level)))]
    (st/set-val! msg-key msg)
    (st/register! mp-id struct no-idx func callback level)))
