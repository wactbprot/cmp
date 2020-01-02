(ns cmp.reg
  ^{:author "wactbprot"
    :doc "Provides a little register deregister interface."}
  (:require [taoensso.timbre :as timbre]
            [cmp.st-mem :as st]))

;;------------------------------
;; listeners 
;;------------------------------
(def listeners (atom {}))

;;------------------------------
;;register!, registered?, de-register!
;;------------------------------
(defn gen-reg-key
  "generates a registration key for the listener atom."
  [mp-id struct]
  (str mp-id "_" struct))

(defn registered?
  "Checks if a `listener` is registered under
  the `listeners`-atom."
  [reg-key]
  (contains? (deref listeners) reg-key))

(defn register!
  "Generates a `ctrl` listener and registers him
  under the key `mp-id` in the `listeners` atom.
  The callback function dispatches depending on
  the result."
  [mp-id struct callback]
  (let [reg-key (gen-reg-key  mp-id  struct)]
        (cond
          (registered? reg-key) (timbre/info "a ctrl listener for "
                                             mp-id
                                             " is already registered!") 
          :else (swap! listeners  assoc
                       reg-key
                       (st/gen-listener mp-id struct
                                        callback)))))

(defn de-register!
  "De-registers the listener with the
  key `mp-id` in the `listeners` atom."
  [mp-id struct]
  (let [reg-key (gen-reg-key mp-id struct)]
    (cond
      (registered? reg-key) (do
                              (st/close-listener! ((deref listeners) reg-key))
                              (swap! listeners dissoc reg-key))
      :else (timbre/info "a ctrl listener for "
                         reg-key
                         " is not registered!"))))
