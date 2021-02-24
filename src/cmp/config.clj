
(ns cmp.config
  (:require [clojure.edn :as edn]))

(defn config
  "Reads a `edn` configuration in file `f`." 
  ([]
   (config "resources/config.edn"))
   ([f]
    (-> f slurp edn/read-string)))

(defn lt-url
  [c]
  (let [usr  (System/getenv "CAL_USR")
        pwd  (System/getenv "CAL_PWD")
        cred (when (and usr pwd) (str usr ":" pwd "@"))]
        (str (:lt-prot c) "://" cred  (:lt-srv c)":"(:lt-port c))) ) 
  
(defn lt-conn [c] (str (lt-url c) "/"(:lt-db c)))

(defn st-conn [c](:st-conn c))

(defn key-pad-length [c] (:key-pad-length c))

(defn st-db [c] (get-in c [:st-conn :spec :db]))

(defn ref-mpd [c] (:ref-mpd c))

(defn min-task-period [c] (:min-task-period c))

(defn json-post-header [c] (:json-post-header c))

(defn dev-hub-url [c] (:dev-hub-url c))

(defn anselm-url [c] (:anselm-url c))

(defn stop-if-delay [c] (:stop-if-delay c))

(defn max-retry [c] (:max-retry c))
