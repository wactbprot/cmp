(ns cmp.core
  (:require [cmp.lt :as lt]
            [cmp.st :as st]
            [cmp.utils :as u]
            [cmp.doc :as d]
            [cmp.build :as b]
            [cmp.check :as check]
            [cmp.poll :as poll]
            [taoensso.timbre :as log])
  (:gen-class)
  (:use [clojure.repl]))

(log/set-level! :debug)

;;------------------------------
;; build
;;------------------------------
(defn build-mp
  "Loads document from long term memory and
  fetches it to short term memory"
  [mp-id]
  (b/store (lt/get-doc (u/compl-main-path mp-id))))

;;------------------------------
;; clear
;;------------------------------
(defn clear-mp
  "Clears all short term memory for the given mp-id"
  [mp-id]
  (st/clear (u/extr-main-path mp-id)))

;;------------------------------
;; documents
;;------------------------------
(defn doc-add
  "Adds a doc to the api to store the resuls in."
  [mp-id doc-id]
  (d/add (u/extr-main-path mp-id) doc-id))

(defn doc-del
  "Removes a doc from the api."
  [mp-id doc-id]
  (d/del (u/extr-main-path mp-id) doc-id))

;;------------------------------
;; check mp tasks
;;------------------------------
(defn check-mp
  "Check and runs the tasks of the container and definitions"
  [mp-id]
  (let [p (u/extr-main-path mp-id)
        n-cont (st/get-val-int (u/get-meta-ncont-path p))
        n-defins (st/get-val-int (u/get-meta-ndefins-path p))]
    (run!
     (fn [i]
       (check/struct (u/get-cont-defin-path p i)))
     (range n-cont))
    (run!
     (fn [i]
       (check/struct (u/get-defins-defin-path p i)))
     (range n-defins))))

;;------------------------------
;; start polling
;;------------------------------
(defn start-mp
  "Check and runs the tasks of the container and definitions"
  [mp-id]
  (let [p (u/extr-main-path mp-id)
        n-cont (st/get-val-int (u/get-meta-ncont-path p))
        n-defins (st/get-val-int (u/get-meta-ndefins-path p))]
    (run!
     (fn [i]
       (poll/start (u/get-cont-ctrl-path p i)))
     (range n-cont))
    (run!
     (fn [i]
       (poll/start (u/get-defins-ctrl-path p i)))
     (range n-defins))))

;;------------------------------
;; stop all polling
;;------------------------------
(defn stop-mp
  "Check and runs the tasks of the container and definitions"
  [mp-id]
  (let [p (u/extr-main-path mp-id)
        n-cont (st/get-val-int (u/get-meta-ncont-path p))
        n-defins (st/get-val-int (u/get-meta-ndefins-path p))]
    (run!
     (fn [i]
       (poll/stop (u/get-cont-ctrl-path p i)))
     (range n-cont))
    (run!
     (fn [i]
       (poll/stop (u/get-defins-ctrl-path p i)))
     (range n-defins))))

;;------------------------------
;; push ctrl commands
;;------------------------------
(defn push-ctrl-defins-cmd
  "Pushes the command to the ith definition."
  [mp-id i cmd]
  (let [p (u/get-defins-ctrl-path (u/extr-main-path mp-id) i)]
    (st/set-val! p  cmd)))

(defn push-ctrl-cont-cmd
  "Pushes the command to the ith container."
  [mp-id i cmd]
  (let [p (u/get-cont-ctrl-path (u/extr-main-path mp-id) i)]
    (st/set-val! p cmd)))
 