(ns cmp.utils
  (:require [clojure.string :as string]
            [clojure.walk :as walk]
            [clj-time.core :as tm]
            [clj-time.format :as tm-f]
            [clojure.data.json :as json])
  (:use [clojure.repl])
  (:gen-class))

(def sep
  "Short-term-database (st) path seperator.
  Must not be a regex operator"
  "@")

(def date-f (tm-f/formatters :date))
(def hour-f (tm-f/formatter "HH"))
(def min-f (tm-f/formatter "mm"))
(def sec-f (tm-f/formatter "ss"))
(def year-f (tm-f/formatter "YYYY"))
(def month-f (tm-f/formatter "MM"))
(def day-f (tm-f/formatter "dd"))

(defn get-date-object []
  (tm/now))

(defn get-date [d]
  (tm-f/unparse date-f d))

(defn get-hour [d]
  (tm-f/unparse hour-f d))

(defn get-min [d]
  (tm-f/unparse min-f d))

(defn get-sec [d]
  (tm-f/unparse sec-f d))

(defn get-day [d]
  (tm-f/unparse day-f d))

(defn get-month [d]
  (tm-f/unparse month-f d))

(defn get-year [d]
  (tm-f/unparse year-f d))

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
