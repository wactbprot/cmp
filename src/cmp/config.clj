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
  (let [usr (System/getenv "CAL_USR")
        pwd (System/getenv "CAL_PWD")]
    (if (and usr pwd)
      (str (:lt-prot c)"://"usr":"pwd"@"(:lt-srv c)":"(:lt-port c))
      (str (:lt-prot c)"://"(:lt-srv c)":"(:lt-port c)))))

(defn lt-conn [c] (str (lt-url c) "/"(:lt-db c)))

(defn st-conn [c](:st-conn c))

(defn key-pad-length [c] (:key-pad-length c))

(defn st-db [c] (get-in c [:st-conn :spec :db]))

(defn edn-tasks [c] (:edn-tasks c))

(defn edn-mpds [c] (:edn-mpds c))

(defn min-task-period [c] (:min-task-period c))

(defn json-post-header [c] (:json-post-header c))

(defn dev-hub-url [c] (:dev-hub-url c))

(defn anselm-url [c] (:anselm-url c))
