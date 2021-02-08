(ns cmp.api
      ^{:author "wactbprot"
        :doc "api for cmp info and ctrl."}
  (:require [cheshire.core           :as che]
            [cmp.exchange            :as exch]
            [cmp.doc                 :as doc]
            [cmp.key-utils           :as ku]
            [cmp.lt-mem              :as lt]
            [cmp.st-mem              :as st]
            [cmp.utils               :as u]
            [com.brunobonacci.mulog  :as mu]))



(defn listeners
  "Returns the `reg-key` and the `id` of the running listeners.

  Example:
  ```clojure
  (listener {} {})
  ```"
  [conf req]
  (let [ls @st/listeners]
    (mapv (fn [k] {:reg-key k
                   :id      (get-in ls [k :id])}) (keys ls))))
