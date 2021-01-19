(ns cmp.worker.devhub
  ^{:author "wactbprot"
    :doc "The devhub worker."}
  (:require [clj-http.client :as http]
            [cmp.config :as cfg]
            [cmp.resp :as resp]
            [cmp.st-mem :as st]
            [cmp.utils :as u]
            [cmp.key-utils :as ku]
            [taoensso.timbre :as log]
            [cmp.worker.pre-script :as ps]))
 
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
      (log/debug "try to send req to: " url)
      (st/set-val! request-key json-task)
      (log/debug "stored json task to: " request-key)
      (try
        (resp/check (http/post url req) task state-key)
        (catch Exception e
          (st/set-state! state-key :error)
          (log/error "request failed"))))))
  
