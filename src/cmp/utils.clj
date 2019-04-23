(ns cmp.utils
  (:require [clojure.string :as string]
            [clojure.data.json :as json])
  (:gen-class))

(def sep "@")

(defn extr-main-path [id]
  (second (re-matches  #"^mpd-([a-z0-3\-_]*)$" id)))

(defn gen-key [path-vec]
  (string/join sep path-vec))

(defn gen-value [val-map]
  (json/write-str val-map))

(defn replace-key-level [level key replacement]
  (gen-key
   (assoc (string/split  key (re-pattern sep)) level replacement)))

