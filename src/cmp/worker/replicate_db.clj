(ns cmp.worker.replicate-db
  ^{:author "wactbprot"
    :doc "Worker to replicate a couchdb database."}
  (:require [clj-http.client :as http]
            [cmp.config :as cfg]
            [cmp.resp :as resp]
            [cmp.st-mem :as st]
            [cmp.utils :as u]
            [taoensso.timbre :as log]))


(defn replicate!
  "Replicate a couchdb database by posting:

  ```json
  {
    \"_id\": \"my_rep\",
    \"source\": \"http://myserver.com/foo\",
    \"target\":  \"http://user:pass@localhost:5984/bar\",
    \"create_target\":  true,
    \"continuous\": true
  }
  ```
  to the `/_replicate` endpoint."
  [task]
  (let [{state-key :StateKey
         source    :SourceDB 
         target    :TargetDB} task]
    (st/set-state! state-key :working)
    (let [url (str (cfg/lt-url (cfg/config)) "/_replicate")
          req (assoc (cfg/json-post-header (cfg/config))
                     :body
                     (u/map->json {:source source :target target}))]
      (try
        (resp/check (http/post url req) task state-key)
        (catch Exception e
          (st/set-state! state-key :error)
          (log/error "get request to url: " url "failed"))))))
