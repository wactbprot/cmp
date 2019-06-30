(ns cmp.core
  (:require [cmp.lt :as lt]
            [cmp.st :as st]
            [cmp.utils :as u]
            [cmp.doc :as d]
            [cmp.build :as b]
            [cmp.check :as chk]
            [cmp.poll :as poll]
            [taoensso.timbre :as log])
  (:gen-class)
  (:use [clojure.repl]))

(log/set-level! :debug)

(defn build-mp
  "Loads document from long term memory and fetches it to short term memory"
  [id]
  (b/store (lt/get-doc id)))

(defn clear-mp
  "Clears all short term memory for the given id"
  [id]
  (st/clear (u/extr-main-path id)))

(defn status
  []
  (poll/status))

(defn check-and-run-mp
  "Check and runs the tasks of the container and definitions"
  [id]
  (let [p (u/extr-main-path id)
        n-cont (st/get-val-int (u/get-meta-ncont-path p))
        n-defins (st/get-val-int (u/get-meta-ndefins-path p))]
    (run!
     (fn [i]
       (chk/container p i)
       (poll/start p i)
       )
     (range n-cont))
    (run!
     (fn [i]
       (chk/definitions p i))
     (range n-defins))))

(defn stop-mp
  "Stops the container and definitions."
  [id]
  (let [p (u/extr-main-path id)
        n-cont (st/get-val-int (u/get-meta-ncont-path p))
        n-defins (st/get-val-int (u/get-meta-ndefins-path p))]
    (run!
     (fn [i]
       (poll/stop p i)
       )
     (range n-cont))
    ;(run!
    ; (fn [i]
    ;   (chk/definitions p i))
    ; (range n-defins))
    ))

(defn add-doc
  "Adds a doc to the api to store the resuls in."
  [mp-id doc-id]
  (d/add (u/extr-main-path mp-id) doc-id))

(defn del-doc
  "Removes a doc from the api."
  [mp-id doc-id]
  (d/del (u/extr-main-path mp-id) doc-id))