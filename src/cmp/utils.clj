(ns cmp.utils
  (:require [clojure.string :as string]
            [clojure.data.json :as json])
  (:gen-class))

(def sep ":")

(defn extr-main-path [id]
  (second (re-matches  #"^mpd-([a-z0-3\-_]*)$" id)))

(defn gen-st-key [path-vec]
  (string/join sep path-vec))

(defn gen-st-value [val-map]
  (json/write-str val-map))

(defn get-key-and-map [v]
  {:path (key v) :value (v 1)})