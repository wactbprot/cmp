(ns cmp.utils
  (:require [clojure.string :as string]
            [clojure.walk :as walk]
            [clj-time.core :as tm]
            [clj-time.format :as tm-f]
            [clj-time.coerce :as tm-c]
            [clojure.data.json :as json])
  (:use [clojure.repl])
  (:gen-class))

(def sep
  "Short-term-database (st) path seperator.
  Must not be a regex operator (like . or |)"
  "@")

(def re-sep
  "The regex version of the seperator."
  (re-pattern sep))

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

(defn get-time
  ([]
   (str (tm-c/to-long (get-date-object))))
  ([d]
   (str (tm-c/to-long d))))

(defmulti extr-main-path
  "Should work on mpd-aaa-bbb as well as on aaa-bbb"
  (fn [s] (string/starts-with? s "mpd-")))

(defmethod extr-main-path true
  [s]
  (second
   (re-matches  #"^mpd-([a-z0-3\-_]*)$" s)))

(defmethod extr-main-path false
  [s]
  s)

(defn gen-map [j]
  (json/read-str j :key-fn keyword))

(defn get-ctrl-path
  [p i]
  (vec->key [p "container" i "ctrl"]))

(defn get-state-path
  [p i]
  (vec->key [p "container" i "state"]))

(defn get-id-path
  [p i]
  (vec->key [p "id" i]))

(defn gen-re-from-map-keys
  [m]
  (re-pattern (string/join "|" (keys m))))

(defn apply-to-map-values [f m]
  (into {} (map (fn [[k v]] [k (f v)]) m)))

(defmulti make-map-regexable
  (fn [m] (and (map? m)
               (not (empty? m)))))

(defmethod make-map-regexable false
  [m])

(defmethod make-map-regexable true
  [m]
  (->> m
       (apply-to-map-values str)
       (walk/stringify-keys)))

(defn get-next-ctrl
  "Extracts next command.
  ToDo:
  Enable kind of programming like provided in ssmp:
  load;run;stop --> [load, run, stop]
  load;2:run,stop -->  [load, run, stop, run, stop]"
  [s]
  (first (string/split s #",")))

(defn set-next-ctrl
  [s r]
  (string/join "," (assoc (string/split s #",") 0 r)))

(defn rm-next-ctrl
  [s]
  (string/join ","
               (or
                (not-empty (rest (string/split s #",")))
                ["ready"])))

(defmulti gen-value
  class)

(defmethod gen-value clojure.lang.PersistentArrayMap
  [x]
  (json/write-str x))

(defmethod gen-value clojure.lang.PersistentVector
  [x]
  (json/write-str x))

(defmethod gen-value clojure.lang.PersistentHashMap
  [x]
  (json/write-str x))

(defmethod gen-value java.lang.String
  [x]
  x)

(defmethod gen-value java.lang.Long
  [x]
  x)

(defmethod gen-value clojure.lang.BigInt
  [x]
  x)

(defmethod gen-value java.lang.Double
  [x]
  x)

(defmethod gen-value java.lang.Boolean
  [x]
  x)

(defn replace-key-at-level
  "Generates a new key by replacing an old one at a certain position"
  [l k r]
  (vec->key
   (assoc
    (string/split k re-sep) l r)))


(defn key->seq-idx
  "The index of the sequential step is given at position 4."
  [s]
  ((string/split s re-sep) 4))

(defn id-key->id
  "Returns position 2 of the id key which should be the document id.
  No checks so far."
  [k]
  ((string/split k re-sep) 2))
  
  
(defn vec->key [p]
  (string/join sep p))
