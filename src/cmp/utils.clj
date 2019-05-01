(ns cmp.utils
  (:require [clojure.string :as string]
            [clojure.walk :as walk]
            [clojure.data.json :as json])
  (:use [clojure.repl])
  (:gen-class))

(def sep
  "Short-term-database (st) path seperator.
  Must not be a regex operator"
  "@")

(defn extr-main-path [id]
  (second (re-matches  #"^mpd-([a-z0-3\-_]*)$" id)))

(defn gen-key [path-vec]
  (string/join sep path-vec))

(defn gen-value [val-map]
  (json/write-str val-map))

(defn gen-map [val-json]
  (json/read-str val-json :key-fn keyword))

(defn replace-key-at-level [level key replacement]
  (gen-key
   (assoc (string/split key (re-pattern sep)) level replacement)))

(defn gen-re-from-map-keys [m]
  (let [ks (keys m)]
    (re-pattern (string/join "|" ks))))

(defn apply-to-map-values [f m]
  (into {} (map (fn [[k v]] [k (f v)]) m)))