(ns cmp.worker.get-date
  ^{:author "wactbprot"
    :doc "Worker to create a date entry in documents."}
  (:require [clj-http.client :as http]
            [cmp.config :as cfg]
            [cmp.resp :as resp]
            [cmp.st-mem :as st]
            [cmp.utils :as u]
            [taoensso.timbre :as log]))


