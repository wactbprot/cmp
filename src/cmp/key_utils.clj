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

  May be:
  * <taskname>
  * definitions
  * container
  "
  [k]
  (when (string? k)
    (nth (string/split k u/re-sep) 1 nil)))

(defn key->no-idx
  "Returns an integer corresponding to the given key `container` or
  `definitions` index."
  [k]
  (when (string? k)
    (nth (string/split k u/re-sep) 2 nil)))

(defn key->func
  "Returns the name of the `func`tion for the given key."
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
  `seq-idx`. Hence a fn-rename"
  [k]
  (key->seq-idx k))

(defn key->par-idx
  "Returns an integer corresponding to the givens key parallel index."
  [k]
  (when (string? k)
    (nth (string/split k u/re-sep) 5 nil)))

(defn key->info-map
  "Builds a `state-map` by means of the key structure and
  `st/key->val`.
  
  Todo:
  use this concept everywhere!
  
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

(defn info-map->ctrl-key
  "Converts a `state-map` into the related `ctrl` key."
  [m]
  (when (map? m)
    (u/vec->key [(:mp-id m) (:struct m) (:no-idx m) "ctrl"])))


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

(defn k->ctrl-k
  "Returns the `ctrl`-key for a given key `k`.
  In other words: ensures k to be a `ctrl-key`.

  ```clojure
  (k->ctrl-k \"wait@container@0@state@0@0\")
  ;; \"wait@container@0@ctrl\"
  (k->ctrl-k
    (k->ctrl-k
      (k->ctrl-k \"wait@container@0@state@0@0\")))
    ;; \"wait@container@0@ctrl\"
  ```" 
  [k]
  (u/vec->key [(key->mp-id k)
               (key->struct k)
               (key->no-idx k)
               "ctrl"]))

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
