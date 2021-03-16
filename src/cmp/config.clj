(ns cmp.config
  (:require [clojure.edn     :as edn]
            [clojure.java.io :as io]
            [clojure.string  :as string]))

(defn config
  "Reads a `edn` configuration in file `f`." 
  ([]
   (config (io/resource "config.edn")))
  ([f]
   (-> f slurp edn/read-string)))

(defn ref-mpd [c] (-> (io/resource (:ref-mpd c)) slurp edn/read-string))
  
(defn st-conn [c](:st-conn c))

(defn key-pad-length [c] (:key-pad-length c))

(defn st-db [c] (get-in c [:st-conn :spec :db]))

(defn min-task-period [c] (:min-task-period c))

(defn json-post-header [c] (:json-post-header c))

(defn anselm-url [c] (:anselm-url c))

(defn stop-if-delay [c] (:stop-if-delay c))

(defn max-retry [c] (:max-retry c))

(defn build-on-start [c]
  (if-let [s (System/getenv "CMP_BUILD_ON_START")]
    (string/split s  #"[;,\s]")
    (:build-on-start c)))

(defn dev-hub-url
  [c]
   (if-let [url (System/getenv "CMP_DEVHUB_URL")]
     url
     (:dev-hub-url c)))

(defn dev-proxy-url
  [c]
   (if-let [url (System/getenv "CMP_DEVPROXY_URL")]
     url
     (:dev-proxy-url c)))

(defn lt-url
  [c]
  (let [lt-srv (System/getenv "CMP_LT_SRV")
        usr    (System/getenv "CAL_USR")
        pwd    (System/getenv "CAL_PWD")
        cred   (when (and usr pwd) (str usr ":" pwd "@"))]
        (str (:lt-prot c) "://" cred  (or lt-srv (:lt-srv c)) ":"(:lt-port c))) ) 

(defn lt-conn [c] (str (lt-url c) "/"(:lt-db c)))
