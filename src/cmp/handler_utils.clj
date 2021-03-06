(ns cmp.handler-utils
  ^{:author "wactbprot"
    :doc "api utils for cmp info and ctrl."}
  (:require [cmp.utils          :as u]
            [cmp.worker.ctrl-mp :as ctrl-mp]
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

(defn req->key [req]  (get-in req [:body :key]))

(defn req->value [req]  (get-in req [:body :value]))

(defn req->mp-id [req] (get-in req [:route-params :mp] "*"))

(defn req->seq-idx [req] (get-in req [:route-params :seq] "*"))

(defn req->no-idx
  "Returns the `no-idx` (container number). Uses
  `run-mp/title->no-idx` if the idx rout-param is not `[0-9]*`"
  [req]
  (let [s (get-in req [:route-params :idx])]
    (cond
      (nil? s) "*"
      (re-matches #"[0-9]*" s)  s
      :title  (codec/url-encode (ctrl-mp/title->no-idx (req->mp-id req) s)))))
