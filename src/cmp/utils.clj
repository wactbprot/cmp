(ns cmp.utils
  (:require [clojure.string :as string]
            [clojure.walk :as walk]
            [clj-time.core :as tm]
            [clj-time.format :as tm-f]
            [clj-time.coerce :as tm-c]
            [io.aviso.ansi :as pretty]
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

    
(defn vec->key [p]
  "Joins the vec to a key."
  (string/join sep p))

(defn replace-key-at-level
  "Generates a new key by replacing an old one at a certain position.
  ToDo: the key levels should have a name or keyword."
  [l k r]
  (vec->key
   (assoc
    (string/split k re-sep) l r)))

(defmulti key->seq-idx
  class)

(defmethod key->seq-idx java.lang.String
  [k]
  (Integer/parseInt  ((string/split k re-sep) 4)))

(defmethod key->seq-idx :default
  [k]
  0)

(defmulti key->par-idx
  class)

(defmethod key->par-idx java.lang.String
  [k]
  (Integer/parseInt  ((string/split k re-sep) 5)))

(defmethod key->par-idx :default
  [k]
  0)

(defmulti key->no-idx
  class)

(defmethod key->no-idx java.lang.String
  [k]
  (Integer/parseInt  ((string/split k re-sep) 2)))

(defmethod key->no-idx :default
  [k]
  0)

(defmulti key->mp-name
  class)

(defmethod key->mp-name java.lang.String
  [k]
  ((string/split k re-sep) 0))

(defmethod key->mp-name :default
  [k]
  "")

(defn id-key->id
  "Returns position 2 of the id key which should be the document id.
  No checks so far."
  [k]
  ((string/split k re-sep) 2))

;;------------------------------
;; date time
;;------------------------------
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

(defn gen-map [j]
  (json/read-str j :key-fn keyword))

;;------------------------------
;; path
;;------------------------------
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

(defmulti compl-main-path
  "Should work on mpd-aaa-bbb as well as on aaa-bbb"
  (fn [s] (string/starts-with? s "mpd-")))

(defmethod compl-main-path true
  [s])

(defmethod compl-main-path false
  [s]
  (str "mpd-" s))

;;------------------------------
;; exchange
;;------------------------------
(defn get-exch-prefix
  [p]
  (vec->key [p "exchange"]))

(defn get-exch-path
  [p name]
  (vec->key [(get-exch-prefix p) name]))

;;------------------------------
;; container path
;;------------------------------
(defn get-cont-prefix
  [p]
  (vec->key [p "container"]))

(defn get-cont-title-path
  [p i]
  (vec->key [(get-cont-prefix p) i  "title"]))

(defn get-cont-descr-path
  [p i]
  (vec->key [(get-cont-prefix p) i  "descr"]))

(defn get-cont-ctrl-path
  [p i]
  (vec->key [(get-cont-prefix p) i  "ctrl"]))

(defn get-cont-elem-path
  [p i]
  (vec->key [(get-cont-prefix p) i  "elem"]))

(defn get-cont-defin-path
  ([p i]
   (vec->key [(get-cont-prefix p) i "definition"]))
  ([p i j k]
   (vec->key [(get-cont-prefix p) i "definition" j k])))

(defn get-cont-state-path
  ([p i]
   (vec->key [(get-cont-prefix p) i  "state"]))
  ([p i j k]
   (vec->key [(get-cont-prefix p) i  "state" j k])))

;;------------------------------
;; definitions path
;;------------------------------
(defn get-defins-prefix
  [p]
  (vec->key [p "definitions"]))

(defn get-defins-defin-path
  ([p i]
   (vec->key [(get-defins-prefix p) i "definition"]))
  ([p i j k]
  (vec->key [(get-defins-prefix p) i "definition" j k])))

(defn get-defins-state-path
  ([p i]
   (vec->key [(get-defins-prefix p) i "state"]))
  ([p i j k]
   (vec->key [(get-defins-prefix p) i "state" j k])))

(defn get-defins-cond-path
  [p i j]
  (vec->key [(get-defins-prefix p) i "cond" j]))

(defn get-defins-ctrl-path
  [p i]
  (vec->key [(get-defins-prefix p) i "ctrl"]))

(defn get-defins-descr-path
  [p i]
  (vec->key [(get-defins-prefix p) i "descr"]))

(defn get-defins-class-path
  [p i]
  (vec->key [(get-defins-prefix p) i "class"]))

;;------------------------------
;; id path
;;------------------------------
(defn get-id-path
  [p id]
  (vec->key [p "id" id]))

;;------------------------------
;; meta path
;;------------------------------
(defn get-meta-prefix
  [p]
  (vec->key [p "meta"]))

(defn get-meta-std-path
  [p]
  (vec->key [(get-meta-prefix p) "std"]))

(defn get-meta-name-path
  [p]
  (vec->key [(get-meta-prefix p) "name"]))

(defn get-meta-descr-path
  [p]
  (vec->key [(get-meta-prefix p) "descr"]))

(defn get-meta-ncont-path
  [p]
  (vec->key [(get-meta-prefix p) "ncont"]))

(defn get-meta-ndefins-path
  [p]
  (vec->key [(get-meta-prefix p) "ndefins"]))

;;------------------------------

(defn gen-re-from-map-keys
  [m]
  (re-pattern (string/join "|" (keys m))))

(defn apply-to-map-values
  [f m]
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

;;------------------------------
;; output
;;------------------------------
(defn print-kv
  [k v]
  (println "\t"
           (pretty/bold-yellow (string/replace k re-sep "\t"))
           (pretty/bold-blue "\t|==>\t")
           (pretty/bold-yellow v)))