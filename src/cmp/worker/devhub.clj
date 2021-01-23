(ns cmp.worker.devhub
  ^{:author "wactbprot"
    :doc "The devhub worker."}
  (:require [cmp.config              :as cfg]
            [clj-http.client         :as http]
            [cmp.key-utils           :as ku]
            [com.brunobonacci.mulog  :as mu]
            [cmp.resp                :as resp]
            [cmp.st-mem              :as st]
            [cmp.utils               :as u]))
 
(defn devhub!
  "Sends `:Value` to [devhub](https://wactbprot.github.io/devhub/) which
  resolves `PreScript` and `PostScript`.
  
  ```clojure
   (devhub! {:Action \"TCP\" :Port 23 :Host \"localhost\" :Value \"Hi!\"})
  ```"
  [task]
  (let [state-key (:StateKey task)]
    (st/set-state! state-key :working)
    (let [request-key (ku/key->request-key state-key)
          json-task   (u/map->json task)
          req         (assoc (cfg/json-post-header (cfg/config)) :body json-task)
          url         (cfg/dev-hub-url (cfg/config))]
      (st/set-val! request-key json-task)
      (mu/log ::devhub! :message "stored json task, send req" :key request-key :url url)
      (try
        (resp/check (http/post url req) task state-key)
        (catch Exception e (st/set-state! state-key :error (.getMessage e)))))))
  
