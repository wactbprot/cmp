(ns cmp.ui.ws
  (:require
   [cheshire.core           :as che]
   [com.brunobonacci.mulog  :as mu]
   [org.httpkit.server      :refer [with-channel
                                    on-receive
                                    on-close
                                    send!]]
   [cmp.st-mem               :as st]
   [cmp.ui.index             :as ui]))

(defonce ws-clients (atom {}))

(defn msg-received
  [msg]
  (let [data (che/decode msg)]
    (mu/log ::msg-received :message "msg/data received")))

(defn main
  [conf req]
  (with-channel req channel
    (mu/log ::ws :message "connected")
    (swap! ws-clients assoc channel true)
    (on-receive channel #'msg-received)
    (on-close channel (fn [status]
                        (swap! ws-clients dissoc channel)
                        (mu/log ::ws :message "closed, status" :status status)))))

(defn send-to-ws-clients
  [conf m]
  (doseq [client (keys @ws-clients)]
    (send! client (che/encode m))))

(defn start!
  [conf]
  (st/register! "*" "*" "*" "*"
                (fn [msg]
                  (when-let [k (st/msg->key msg)]
                    (send-to-ws-clients conf {:key (ui/make-selectable k)
                                              :value (st/key->val k)})))
                "c"))

(defn stop! [conf] (st/de-register! "*" "*" "*" "*" "c"))
