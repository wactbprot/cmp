(ns cmp.worker.replicate-db
  ^{:author "wactbprot"
    :doc "Worker to replicate a couchdb database."}
  (:require [cmp.config              :as cfg]
            [clj-http.client         :as http]
            [com.brunobonacci.mulog  :as mu]
            [cmp.resp                :as resp]
            [cmp.st-mem              :as st]
            [cmp.utils               :as u]))

(defn gen-url [] (str (cfg/lt-url (cfg/config)) "/_replicate"))

(defn gen-req
  [{s :SourceDB t :TargetDB}]
  (assoc (cfg/json-post-header (cfg/config))
         :body (u/map->json {:source s :target t})))

(defn replicate!
  "Replicate a database (CouchDB) by posting:

  ```json
  {
    \"_id\": \"my_rep\",
    \"source\": \"http://myserver.com/foo\",
    \"target\":  \"http://user:pass@localhost:5984/bar\"
  }
  ```
  to the `/_replicate` endpoint."
  [task]
  (let [{state-key :StateKey} task]
    (st/set-state! state-key :working)
    (try
      (resp/check (http/post (gen-url) (gen-req task)) task state-key)
      (catch Exception e (st/set-state! state-key :error (.getMessage e))))))
