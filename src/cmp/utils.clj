(ns cmp.utils
  (:require [clojure.string  :as string]
            [cheshire.core   :as che]
            [clj-time.core   :as tm]
            [clj-time.format :as tm-f]
            [clj-time.coerce :as tm-c]
            [cmp.config      :as cfg]
            ))

(def ok-set #{"ok" :ok "true" true "yo!"})

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
  "Generates a new key by replacing an old key `k` at the given position
  `l` with the given string `r`.

  REVIEW The key levels should have a name or keyword.  Passing
  integers (`l`) is unimaginative.
  "
  [l k r]
  {:pre [(string? k)
         (int? l)
         (string? r)]}
  (let [v (string/split k re-sep)]
    (when (< l (count v)) (vec->key (assoc v l r)))))

(defn key-at-level
  "Returns the value of the key `k` at the level `l`."
  [k l]
  {:pre [(string? k)
         (int? l)] }
  (nth (string/split k re-sep) l nil))

;;------------------------------
;; date time
;;------------------------------
(defn get-date-object [] (tm/now))
(defn get-hour  [d] (tm-f/unparse (tm-f/formatter "HH")   d))
(defn get-min   [d] (tm-f/unparse (tm-f/formatter "mm")   d))
(defn get-sec   [d] (tm-f/unparse (tm-f/formatter "ss")   d))
(defn get-day   [d] (tm-f/unparse (tm-f/formatter "dd")   d))
(defn get-month [d] (tm-f/unparse (tm-f/formatter "MM")   d))
(defn get-year  [d] (tm-f/unparse (tm-f/formatter "YYYY") d))

(defn get-date 
  ([]
   (get-date (get-date-object)))
  ([d]
   (tm-f/unparse (tm-f/formatters :date) d)))

(defn get-time
  ([]
   (str (tm-c/to-long (get-date-object))))
  ([d]
   (str (tm-c/to-long d))))

(defn short-string
  "Short a `string` `s` to `n` or `45` chars. Returns `nil` is `s` is
  not a `string`."
  ([s]
   (when (string? s) (short-string s 40)))
  ([s n]
   (when (string? s) (if (< (count s) n) s (str (subs s 0 n) "...")))))
   
(defn ensure-int
  "Ensures `i` to be integer. Returns 0 as default.

  ```clojure
  (u/ensure-int 100)
  ;; 100
  (u/ensure-int \"w\")
  ;; 0
  (u/ensure-int \"00\")
  ;; 0
  (u/ensure-int \"10\")
  ;; 10
  (u/ensure-int true)
  ;; 0
  ```
  "
  [i]
  (if (integer? i)
    i
    (try
      (Integer/parseInt i)
      (catch Exception e
        0))))

(defn pad-ok?
  "Checks if the padding of `i` is ok. `\"*\"` serves pattern matching."
  ([i]
   (pad-ok? i (cfg/key-pad-length (cfg/config))))
  ([i n]
   (cond
     (= i "*")         true 
     (and
      (string? i)
      (= n (count i))) true
     :else             false)))

(defn lp
  "Left pad the given number if it is not a string. Default
  is `(cfg/key-pad-length (cfg/config))`.

  Example:
  ```clojure
   (u/lp 2)
  ;; \"002\"
  (u/lp \"02\")
  ;; \"002\"
  (u/lp 2)
  ;; \"002\"
  (u/lp true)
  ;; \"000\"
  (u/lp \"003\")
  ;; \"003\"
  (u/lp \"000003\")
  ;; \"003\"
  ```
  "
  ([i]
   (if (pad-ok? i) i (lp (ensure-int i) 3)))
  ([i n]
   (if (pad-ok? i) i (format (str "%0" n "d") (ensure-int i)))))

;;------------------------------
;; mp-id
;;------------------------------
(defn extr-main-path
  "Extracts the main path. Should work on `mpd-aaa-bbb` as well as on
  `aaa-bbb`.

  ```clojure
  (u/extr-main-path \"aa\")
  ;; \"aa\"
  (u/extr-main-path \"aa-bbb\")
  ;; \"aa-bbb\"
  (u/extr-main-path \"aa-bbb-lll\")
  ;; \"aa-bbb-lll\"
  ```
  "
  [s]
  (let [p "mpd-"]
    (if (string/starts-with? s p) (string/replace  s (re-pattern p) "") s)))

(defn compl-main-path
  "Completes the main path by padding a `mpd-` in case it is missing.
  
  ```clojure
  (u/compl-main-path \"aaa\")
  ;; \"mpd-aaa\"
  (u/compl-main-path \"mpd-aaa\")
  ;; \"mpd-aaa\"
  ```
  "
  [s]
  (if (string/starts-with? s "mpd-") s (str "mpd-" s)))

;;------------------------------

(defn apply-to-map-values
  "Applies function `f` to the values of the map `m`."
  [f m]
  (into {}
        (map (fn [[k v]]
              (cond
                (map? v) [k (apply-to-map-values f v)]
                :default [k (f v)]))
             m)))

(defn apply-to-map-keys
  "Applies function `f` to the keys of the map `m`."
  [f m]
  (into {}
        (map (fn [[k v]]
               (cond
                 (map? v) [(f k) (apply-to-map-keys f v)]
                 :default [(f k) v]))
             m)))
;;------------------------------
;; doc, json, map
;;------------------------------
(defn map->json
  "Transforms a hash-map  to a json string"
  [m]
  (che/generate-string m))

(defn json->map
  "Transforms a json object to a map."
  [j]
  (che/parse-string j true))

(defn doc->safe-doc
  "Replaces all of the `@`-signs (if followed by letters 1)
  by a `%`-sign  because `:%kw` is a valid keyword but `:@kw` not
  (or at least problematic).

  1) There are devices annotating channeles by `(@101:105)`.
  This should remain as it is.
  "
  [doc]
  (json->map (string/replace (map->json doc)  #"(@)([a-zA-Z])" "%$2")))

;;------------------------------
;; ctrl endpoint -> poll and run
;;------------------------------
(defn next-ctrl-cmd
  "Extracts next command.

  #TODO:  Enable kind of programming like provided in ssmp:

  * `load;run;stop` --> `[load, run, stop]`
  * `load;2:run,stop` -->  `[load, run, stop, run, stop]`"
  [s]
  (cond
    (nil? s) :stop
    :default (first (string/split s #","))))
