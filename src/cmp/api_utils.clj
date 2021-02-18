(ns cmp.api-utils
      ^{:author "wactbprot"
        :doc "api utils for cmp info and ctrl."}
  (:require [cmp.utils     :as u]
            [cmp.key-utils :as ku]
            [cmp.st-mem    :as st]))

(defn key-value-map
  "Generates a map consisting of `k`ey and `v`alue. The result is
  merged with `m`."
  ([k]
   (key-value-map k nil))
  ([k m]
   (merge (assoc (ku/key->info-map k) :value (st/key->val k) :key k) m)))

(defn req->mp-id  [req] (get-in req [:route-params :mp] "*"))
(defn req->no-idx [req] (get-in req [:route-params :idx] "*"))
