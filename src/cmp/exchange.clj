(ns cmp.exchange
  ^{:author "wactbprot"
    :doc "Handles the access to the exchange interface."}
  (:require [cmp.key-utils          :as ku]
            [com.brunobonacci.mulog :as mu]
            [clojure.string         :as string]
            [cmp.st-mem             :as st]
            [cmp.utils              :as u]))

(defn exch-key
  "Returns the base key for the exchange path.

  ```clojure
  (exch-key  \"foo\" \"bar.baz\")
  ;; \"foo@exchange@bar\"
  (exch-key \"foo\" \"bar\")
  ;; \"foo@exchange@bar\"
  ```
  "
  [mp-id s]
  {:pre [(not (nil? s))]}
  (ku/exch-key mp-id (first (string/split s #"\."))))

(defn key->second-kw
  "Returns the keyword or nil.

  ```clojure
  (key->second-kw \"foo\" )
  ;; nil
  (key->second-kw \"foo.bar\" )
  ;; :bar
  ```"  
  [s]
  (when-let [x (second (string/split s #"\."))] (keyword x)))

(defn key->first-kw
  "Returns the keyword or nil.

  ```clojure
  (key->second-kw \"foo\" )
  ;; nil
  (key->second-kw \"foo.bar\" )
  ;; :bar
  ```"  
  [s]
  (when-let [x (first (string/split s #"\."))] (keyword x)))

(defn read!
  "Returns e.g the *compare value* belonging to a `mp-id` and an
  ExchangePath `k`. First try is to simply request to
  `<mp-id>@exchange@<k>`. If this is `nil` Second try is to get the
  *keyword* `kw` from `k` if `k` looks like this: `aaa.bbb`. If `kw`
  is not `nil` it is used to extract the related value.

  ```clojure
  (read! \"ref\" \"A.Unit\")
  ;; \"Pa\"
  ;; or:
  (read! \"devhub\" \"Vraw_block1\")
  ;; [1 0 1 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 1 0]
  ```"
  [mp-id p]
  (if-let [val-p (st/key->val (ku/exch-key mp-id p))]
    val-p
    (let [val-k (st/key->val (exch-key mp-id p))]
      (if-let [kw (key->second-kw p)]
        (kw val-k)
        val-k))))

(defn from!
  "Builds a map by replacing the values of the input map `m`.
  The replacements are gathered from the `exchange` interface with the
  keys: `<mp-id>@exchange@<input-map-value>`

  The example key: `ref@exchange@Vraw_block1` with the example value:
  `[1 0 1 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 1 0]` should return:
  `{:%stateblock1 [1 0 1 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 1 0]}`

  
  ```clojure
  (from! \"ref\" {:%stateblock1 \"Vraw_block1\"})
  ;; =>
  ;; {:%stateblock1 [1 0 1 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 1 0]}
  ```
  
  **Todo**

  Check for non trivial `<input-map-value>` like
  `{:%aaa \"bbb.ccc\"}`
  "
  [mp-id m]
  (when (and (string? mp-id) (map? m))
    (u/apply-to-map-values
     (fn [v] (read! mp-id v))
     m)))

(defn enclose-map
  "Encloses the given map `m` with respect to the path `p`.

  Example:
  ```clojure
  (enclose-map {:gg \"ff\"} \"mm.ll\")
  ;; gives:
  ;; {:mm {:ll {:gg \"ff\"}}}

  (enclose-map {:gg \"ff\"} \"mm\")
  ;; gives:
  ;; {:mm {:gg \"ff\"}}

  (enclose-map {:gg \"ff\"} nil)
  ;; gives:
  ;; {:gg \"ff\"}
  ```
  "
  [m p]
  (if-not p
    m
    (let [a (key->first-kw p)]
      (if-let [b (key->second-kw p)]
        {a {b m}}
        {a m}))))

(defn to!
  "Writes `m` to the exchange interface.  The first level keys of `m`
  are used for the key. The return value of the storing
  process (e.g. \"OK\") is converted to a `keyword`. After storing the
  amounts of `:OK` is compared to `(count m)`.

  Example:
  ```clojure
  {:A 1
   :B 2}
  ```
  Stores the value `1` under the key
  `<mp-id>@exchange@A` and a `2` under
  `<mp-id>@exchange@B`.

  If a path `p` is given the `enclose-map` function
  respects `p`.
  "
  ([mp-id m p]
   (to! mp-id (enclose-map m p)))
  ([mp-id m]
   (if (string? mp-id)
    (if (map? m)
      (let [res (map
                 (fn [[k v]]
                   (keyword (st/set-val! (ku/exch-key mp-id (name k)) v)))
                 m)]
        (if (= (count m) (:OK (frequencies res)))
          {:ok true}
          {:error "not all write processes succeed"}))
      {:ok true})
    {:error "mp-id must be a string"})))

(defn ok?
  "Checks a certain exchange endpoint to evaluate
  to true"
  [mp-id k]
  (contains? u/ok-set (read! mp-id k)))

(defn stop-if
  "Checks if the exchange path given with `:MpName` and `:StopIf`
  evaluates to true."
  [{mp-id :MpName k :StopIf}]
  (if k
    (ok? mp-id k)
    true))

(defn run-if
  "Checks if the exchange path given with `:MpName` and `:RunIf`
  evaluates to true."
  [{mp-id :MpName k :RunIf}]
  (if k
    (ok? mp-id k)
    true))
