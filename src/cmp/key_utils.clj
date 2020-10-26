(ns cmp.key-utils
  ^{:author "wactbprot"
    :doc "All about key transformation, -arithmetic and -info."}
  (:require [taoensso.timbre :as log]
            [clojure.string  :as string]
            [cmp.st-mem      :as st]
            [cmp.utils       :as u]))

;;------------------------------
;; key arithmetic
;;------------------------------
(defn key->mp-id
  "Returns the name of the key space for the given key.

  May be:
  * tasks
  * <mp-id>

  "
  [k]
  (when (and
         (string? k)
         (not (empty? k)))
    (nth (string/split k u/re-sep) 0 nil)))

(defn key->struct
  "Returns the name of the `struct`ure for the given key.
  The structure is the name of the key at the second
  position. Possible values are:
  
  * `<taskname>`
  * `definitions`
  * `container`
  * `exchange`
  * `id`
  * `meta`
  "
  [k]
  (when (string? k)
    (nth (string/split k u/re-sep) 1 nil)))

(defn key->no-idx
  "Returns the value of the key corresponding to the given key
  `container` or `definitions` index."
  [k]
  (when (string? k)
    (nth (string/split k u/re-sep) 2 nil)))

(defn key->func
  "Returns the name of the `func`tion for the given key.
  Possible values are:

  * `ctrl`
  * `state`
  * `request`
  * `response`
  * `elem`
  * `decr`
  * `title`
  * `definition`
  "
  [k]
  (when (string? k)
    (nth (string/split k u/re-sep) 3 nil)))

(defn key->seq-idx
  "Returns an integer corresponding to the givens key sequential index."
  [k]
  (when (string? k)
    (nth (string/split k u/re-sep) 4 nil)))

(defn key->no-jdx
  "The 4th position at definitions has nothing todo with
  `seq-idx` (function rename)"
  [k]
  (key->seq-idx k))

(defn key->par-idx
  "Returns an integer corresponding to the givens key parallel index."
  [k]
  (when (string? k)
    (nth (string/split k u/re-sep) 5 nil)))

(defn key->info-map
  "Builds a `info-map` by means of the key structure and
  `st/key->val`.
  
  Example:
  ```clojure
  (key->info-map \"\") 
  ;;{:mp-id nil,
  ;; :struct nil,
  ;; :no-idx nil,
  ;; :func nil,
  ;; :seq-idx nil,
  ;; :par-idx nil}
  ```"
  [k]
  (when (string? k)
    {:mp-id   (key->mp-id   k)
     :struct  (key->struct  k)
     :func    (key->func    k)
     :no-idx  (key->no-idx  k)
     :seq-idx (key->seq-idx k)
     :par-idx (key->par-idx k)}))

(defn info-map->definition-key
  "Converts a `state-map` into the related `definition` key."
  [m]
  (when (map? m)
    (u/vec->key [(:mp-id m) (:struct m) (:no-idx m) "definition"
                 (:seq-idx m) (:par-idx m)])))

(defn k->state-ks
  "Returns the state keys for a given path.

  ```clojure
  (k->state-ks \"wait@container@0\")
  ```" 
  [k]
  (when k
    (sort
     (st/key->keys
      (u/vec->key [(key->mp-id k)
                   (key->struct k)
                   (key->no-idx k)
                   "state"])))))

(defn seq-idx->all-par
  "Returns a vector of [[info-maps]] with all `par` steps for a given
  `i`.

  Example:
  ```clojure
    (seq-idx->all-par [{:seq-idx 0 :par-idx 0 }
                       {:seq-idx 1 :par-idx 0 }
                       {:seq-idx 1 :par-idx 1 }] 1)
  ;; =>
  ;; [{:seq-idx 1 :par-idx 0 :state :ready}
  ;;  {:seq-idx 1 :par-idx 1 :state :ready}]
  ```"
  [v i]
  (filterv (fn [m] (= (u/ensure-int i)
                      (u/ensure-int (:seq-idx m)))) v))

(defn key->response-key
  "Turns the given `state-key` into a `response-key`.

    Example:
  ```clojure
  (key->response-key \"ref@container@0@state@0@0\")
  ;; devs@container@0@response@0@0
  ```
  "
  [k]
  (u/replace-key-at-level 3 k "response"))

(defn key->request-key
  "Turns the given `state-key` into a `request-key` This key is used to
  store the assembled task right before it is started off.

  Example:
  ```clojure
  (key->response-key \"ref@container@0@state@0@0\")
  ;; devs@container@0@response@0@0
  ```
  "
  [k]
  (u/replace-key-at-level 3 k "request"))


;;------------------------------
;; message
;;------------------------------
(defn key->message-path
  "Returns the `message` path."
  [k]
  (u/vec->key [(key->mp-id k)
               (key->struct k)
               (key->no-idx k)
               "message"]))


;;------------------------------
;; ctrl-key
;;------------------------------
(defn key->ctrl-key
  "Returns the `ctrl`-key for a given key `k`.
  In other words: ensures k to be a `ctrl-key`.

  ```clojure
  (key->ctrl-key \"wait@container@0@state@0@0\")
  ;; \"wait@container@0@ctrl\"
  (key->ctrl-key
    (key->ctrl-key
      (key->ctrl-key \"wait@container@0@state@0@0\")))
    ;; \"wait@container@0@ctrl\"
  ```" 
  [k]
  (u/vec->key [(key->mp-id k)
               (key->struct k)
               (key->no-idx k)
               "ctrl"]))

(defn info-map->ctrl-key
  "Converts a `state-map` into the related `ctrl` key."
  [m]
  (when (map? m)
    (u/vec->key [(:mp-id m) (:struct m) (:no-idx m) "ctrl"])))


;;------------------------------
;; exchange
;;------------------------------
(defn exch-prefix
  "Returns the `exchange` prefix."
  [mp-id]
  (when (string? mp-id)
  (u/vec->key [mp-id "exchange"])))

(defn exch-key
  "Returns the `exchange` path (key)."
  [mp-id s]
  (u/vec->key [(exch-prefix mp-id) s]))


;;------------------------------
;; container path
;;------------------------------
(defn cont-prefix
  "Returns the `container` prefix."
  [mp-id]
  (u/vec->key [mp-id "container"]))

(defn cont-title-key
  [mp-id i]
  (u/vec->key [(cont-prefix mp-id) (u/lp i)  "title"]))

(defn cont-descr-key
  [mp-id i]
  (u/vec->key [(cont-prefix mp-id) (u/lp i)  "descr"]))

(defn cont-ctrl-key
  [mp-id i]
  (u/vec->key [(cont-prefix mp-id) (u/lp i)  "ctrl"]))

(defn cont-elem-key
  [mp-id i]
  (u/vec->key [(cont-prefix mp-id) (u/lp i)  "elem"]))

(defn cont-defin-key
  ([mp-id i]
   (u/vec->key [(cont-prefix mp-id) (u/lp i) "definition"]))
  ([mp-id i j k]
   (u/vec->key [(cont-prefix mp-id) (u/lp i) "definition" (u/lp j) (u/lp k)])))

(defn cont-state-key
  ([mp-id i]
   (u/vec->key [(cont-prefix mp-id) (u/lp i)  "state"]))
  ([mp-id i j k]
   (u/vec->key [(cont-prefix mp-id) (u/lp i)  "state" (u/lp j) (u/lp k)])))


;;------------------------------
;; definitions path
;;------------------------------
(defn defins-prefix
  "Returns the `definitions` prefix."
  [mp-id]
  (u/vec->key [mp-id "definitions"]))

(defn defins-defin-key
  ([mp-id i]
   (u/vec->key [(defins-prefix mp-id) (u/lp i) "definition"]))
  ([mp-id i j k]
  (u/vec->key [(defins-prefix mp-id) (u/lp i) "definition" (u/lp j) (u/lp k)])))

(defn defins-state-key
  ([mp-id i]
   (u/vec->key [(defins-prefix mp-id) (u/lp i) "state"]))
  ([mp-id i j k]
   (u/vec->key [(defins-prefix mp-id) (u/lp i) "state" (u/lp j) (u/lp k)])))

(defn defins-cond-key
  ([mp-id i]
  (u/vec->key [(defins-prefix mp-id) (u/lp i) "cond"]))
  ([mp-id i j]
  (u/vec->key [(defins-prefix mp-id) (u/lp i) "cond" (u/lp j)])))

(defn defins-ctrl-key
  [mp-id i]
  (u/vec->key [(defins-prefix mp-id) (u/lp i) "ctrl"]))

(defn defins-descr-key
  [mp-id i]
  (u/vec->key [(defins-prefix mp-id) (u/lp i) "descr"]))

(defn defins-class-key
  [mp-id i]
  (u/vec->key [(defins-prefix mp-id) (u/lp i) "class"]))


;;------------------------------
;; id key
;;------------------------------
(defn id-prefix
  "Returns the `id` prefix."
  [mp-id]
  (u/vec->key [mp-id "id"]))

(defn id-key
  "Returns the `id` key."
  [mp-id id]
  (u/vec->key [(id-prefix mp-id) id]))


;;------------------------------
;; meta path
;;------------------------------
(defn meta-prefix
  "Returns the `meta` prefix."
  [mp-id]
  (u/vec->key [mp-id "meta"]))

(defn meta-std-key
  [mp-id]
  (u/vec->key [(meta-prefix mp-id) "std"]))

(defn meta-name-key
  [mp-id]
  (u/vec->key [(meta-prefix mp-id) "name"]))

(defn meta-descr-key
  [mp-id]
  (u/vec->key [(meta-prefix mp-id) "descr"]))

(defn meta-ncont-key
  [mp-id]
  (u/vec->key [(meta-prefix mp-id) "ncont"]))

(defn meta-ndefins-key
  [mp-id]
  (u/vec->key [(meta-prefix mp-id) "ndefins"]))
