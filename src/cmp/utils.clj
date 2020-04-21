(ns cmp.utils
  (:require [clojure.string :as string]
            [clojure.walk :as walk]
            [clj-time.core :as tm]
            [clj-time.format :as tm-f]
            [clj-time.coerce :as tm-c]
            [io.aviso.ansi :as pretty]
            [clojure.data.json :as json]))

(def sep
  "Short-term-database (st) path seperator.
  Must not be a regex operator (like `.` or `|`)"
  "@")

(def re-sep
  "The regex version of the seperator."
  (re-pattern sep))

(defn vec->key
  "Joins the vec to a key."
  [p]
  (string/join sep p))

(defn replace-key-at-level
  "Generates a new key by replacing an
  old key `k`  at the given position `l` with the
  given string `r`.

  REVIEW
  The key levels should have a name or keyword.
  Passing integers (`l`) is unimaginative. 
  "
  [l k r]
  {:pre [(string? k)
         (int? l)
         (string? r)]}
  (vec->key (assoc (string/split k re-sep) l r)))

(defn key-at-level
  "Returns the value of the key `k` at the level `l`"
  [k l]
  {:pre [(string? k)
         (int? l)] }
  (nth (string/split k re-sep) l ))


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

;;------------------------------
;; mp-id
;;------------------------------
(defn main-path
  "Extracts the main path.

  Should work on `mpd-aaa-bbb`
  as well as on `aaa-bbb`.

  ```clojure
  (u/main-path \"aa\")
  ;; \"aa\"
  cmp.core> (u/main-path \"aa-bbb\")
  ;; \"aa-bbb\"
  cmp.core> (u/main-path \"aa-bbb-lll\")
  ;; \"aa-bbb-lll\"
  ```
  "
  [s]
  (if (string/starts-with? s "mpd-")
    (second
     (re-matches  #"^mpd-([a-z0-3\-_]*)$" s))
    s))

(defn compl-main-path
  "Completes the main path by padding a `mpd-`
  in case it is missing.
  
  ```clojure
  (u/compl-main-path \"aaa\")
  ;; \"mpd-aaa\"
  (u/compl-main-path \"mpd-aaa\")
  ;; \"mpd-aaa\"
  ```
  "
  [s]
  (if (string/starts-with? s "mpd-")
    s
    (str "mpd-" s)))

;;------------------------------

(defn apply-to-map-values
  "Applies function `f` to the values of
  the map `m`."
  [f m]
  (into {} (map
            (fn [[k v]]
              (cond
                (map? v) [k (apply-to-map-values f v)]
                :default [k (f v)]))
            m)))

(defn apply-to-map-keys
  "Applies function `f` to the keys of
  the map `m`."
  [f m]
  (into {} (map
            (fn [[k v]]
              (cond
                (map? v) [(f k) (apply-to-map-keys f v)]
                :default [(f k) v]))
            m)))

;;------------------------------
;; output
;;------------------------------
(defn print-sep
  []
  (println
   (pretty/bold-white "\t ---- \t\t\t ----")))

(defn print-kv
  [k v]
  (println "\t"
           (pretty/yellow k)
           (pretty/bold-white "\t\t\t : \t\t\t")
           (pretty/yellow v)))

(defn print-kvv
  [k v0 v1]
  (println "\t"
           (pretty/yellow k)
           (pretty/bold-white "\t\t\t : \t\t\t")
           (pretty/yellow v0)
           (pretty/bold-white "\t\t\t : \t\t\t")
           (pretty/yellow v1)))

(defn print-vec-map
  "Pretty prints a vector of maps."
  [vec]
  (run! (fn [m]
          (print-sep)
          (run! (fn [[k v]]
                  (print-kv k v))
                m))
        vec))

;;------------------------------
;; doc, json, map
;;------------------------------
(defn map->json
  "Transforms a hash-map  to a json string"
  [m]
  (json/write-str m))

(defn json->map
  "Transforms a json object to a map."
  [j]
  (json/read-str j :key-fn keyword))

(defn doc->safe-doc
  "Replaces all of the `@`-signs (if followed by letters 1)
  by a `%`-sign  because `:%kw` is a valid keyword but `:@kw` not
  (or at least problematic).

  1) There are devices annotating channeles by `(@101:105)`.
  This should remain as it is.
  "
  [doc]
  (json->map (string/replace (json/write-str doc)  (re-pattern "@\\[a-zA-Z\\]*") "%")))

(defn clj->val
  "Casts the given (complex) value `x` to a writable
  type. `json` is used for complex data types.

  ```clojure
  (st/clj->val {:foo \"bar\"})
  ;; \"{\"foo\":\"bar\"}\"
  (st/clj->val [1 2 3])
  ;; \"[1,2,3]\"
  ```
  "
  [x]
  (condp = (class x)
    clojure.lang.PersistentArrayMap (json/write-str x)
    clojure.lang.PersistentVector   (json/write-str x)
    clojure.lang.PersistentHashMap  (json/write-str x)
    x))


(defn make-replacable
  [x]
  (condp = (class x)
    clojure.lang.PersistentArrayMap (json/write-str x)
    clojure.lang.PersistentVector   (json/write-str x)
    clojure.lang.PersistentHashMap  (json/write-str x)
    (str x)))

;;------------------------------
;; pick
;;------------------------------
(defn val->clj
  "Parses value `v` and returns a
  clojure type of it.

  ```clojure
  (val->clj \"-1e-9\")
  ;; -1.0E-9
  ;; class:
  ;;
  (class (val->clj \"1.23\"))
  ;; java.lang.Double
  (class (val->clj \"a\"))
  ;; java.lang.String
  (class (val->clj \"[]\"))
  ;; clojure.lang.PersistentVector
  (class (val->clj \"{}\"))
  ;; clojure.lang.PersistentArrayMap
  (class (val->clj \"{\"a\":1}\"))
  ;; clojure.lang.PersistentArrayMap
  ```
  "
  [v]
  (let [s-pat #"^-?\d+\.?\d*([Ee]\+\d+|[Ee]-\d+|[Ee]\d+)?$"
        c-pat #"^[\[\{]"]
    (cond
      (nil? v) nil
      (re-find s-pat v) (read-string v)
      (re-find c-pat v) (json->map v)
      :else v)))

;;------------------------------
;; ctrl endpoint -> poll and run
;;------------------------------
(defn get-next-ctrl
  "Extracts next command.

  #TODO:  Enable kind of programming like provided in ssmp:

  * `load;run;stop` --> `[load, run, stop]`
  * `load;2:run,stop` -->  `[load, run, stop, run, stop]`"
  [s]
  (cond
    (nil? s) :stop
    :default (first (string/split s #","))))

(defn val->vec
  "Ensures that `v` is a vector.

  ```clojure
  (val->vec nil) ;!
  ;; nil
  (val->vec 1)
  ;; [1]
  (val->vec [1])
  ;; [1]
  ```"
  [v]
  (if v
    (if (vector? v)
      v
      [v])))

(defn vector-if
  "Makes the value `v` behind the keyword `kw`
  a vector if `v` is not nil."
  [m kw]
  (if (and (map? m) (keyword? kw))
    (if-let [v (kw m)]
      (assoc m kw (val->vec v))
      m)))

(defn replace-if
  "Replaces `v`alue of `k`ey in struct
  if `v`is not `nil`.

  ```clojure
  (replace-if {:Type \"a\"} :Type \"b\")
  ;; {:Type \"b\"}
  ```
  "
  [m k v]
  (if (and (some? v) (keyword? k))
    (assoc m k v)
    m))

(defn append-if
  "Appends `v` to the value of `k`.
  If `k` does not exist in `m`, `k [v]` is assoced.
  If `k` does exist in `m`, `v` is conjed.
  
  ```clojure
  (append-if {:Value [1 2 3]} :Value 4)
  ;; {:Value [1 2 3 4]}
  ```"
  [m k v]
  (if (and (some? v) (keyword? k))
    (let [new-v (val->vec v)]
      (if-let [old-v (k m)]
        (assoc m k (into [] (concat old-v new-v)))
        (assoc m k new-v)))
    m))

(defn path->kw-vec
  "Turns the path into a vector of
  keywords.

  ```clojure
  (path->kw-vec \"a.b.c\")
  ;; [:a :b :c]
  ```"
  [s]
  {:pre [(string? s)]}
  (into []
        (map
         keyword
         (string/split s (re-pattern "\\.")))))
