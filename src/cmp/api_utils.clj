(ns cmp.api-utils
      ^{:author "wactbprot"
        :doc "api utils for cmp info and ctrl."}
  (:require [cmp.utils          :as u]
            [cmp.worker.run-mp  :as run-mp]
            [cmp.st-mem         :as st]
            [cmp.st-utils       :as stu]
            [ring.util.codec    :as codec]))

(defn key-value-map
  "Generates a map consisting of `k`ey and `v`alue. The result is
  merged with `m`."
  ([k]
   (key-value-map k nil))
  ([k m]
   (merge (assoc (stu/key->info-map k) :value (st/key->val k) :key k) m)))

(defn req->mp-id  [req] (get-in req [:route-params :mp] "*"))

(defn req->no-idx
  "Returns the `no-idx` (container number). Uses
  `run-mp/title->no-idx` if the idx rout-param is not `[0-9]*`"
  [req]
  (let [s (get-in req [:route-params :idx])]
    (cond
      (nil? s) "*"
      (re-matches #"[0-9]*" s)  s
      :title  (codec/url-encode (run-mp/title->no-idx (req->mp-id req) s)))))
