(ns cmp.server
    ^{:author "wactbprot"
      :doc "Provides a REST api for cmp info and ctrl."}
  (:require [compojure.route          :as route]
            [com.brunobonacci.mulog   :as mu]
            [cmp.config               :as c]
            [cmp.api                  :as a]
            [cmp.ui.core              :as ui]
            [cmp.ui.listener          :as uil]
            [cmp.ui.container         :as uic]
            [cmp.ui.mp-meta           :as uim]
            [cmp.ui.ws                :as ws]
            [cmp.st-mem               :as st] 
            [compojure.core           :refer :all]
            [compojure.handler        :as handler]
            [org.httpkit.server       :refer [run-server]]
            [ring.middleware.json     :as middleware]
            [ring.util.response       :as res])
    (:use    [clojure.repl]))

(def conf (c/config))

(defonce server (atom nil))

(defonce logger (atom nil))

(defroutes app-routes
  (GET "/config"                             [:as req] (res/response conf))
  (GET "/listeners"                          [:as req] (res/response (a/listeners conf req)))
  (GET "/tasks"                              [:as req] (res/response (a/tasks     conf req)))
  (GET "/:mp/meta"                           [:as req] (res/response (a/mp-meta   conf req)))

  (GET "/ui/listeners"                       [:as req] (uil/view conf req (a/listeners conf req)))
  (GET "/ui"                                 [:as req] (uil/view conf req (a/listeners conf req)))

  (GET "/ui/:mp/meta"                        [:as req] (uim/view conf req (a/mp-meta conf req)))
  (GET "/ui/:mp"                             [:as req] (uim/view conf req (a/mp-meta conf req)))

  (POST "/:mp/container"                     [:as req] (res/response (a/set-val! conf req)))

  (GET "/ui/:mp/container/ctrl"              [:as req] (uic/view-ctrl  conf req (a/container-ctrl       conf req)))
  (GET "/ui/:mp/container/state"             [:as req] (uic/view-state conf req (a/container-state      conf req)))
  (GET "/ui/:mp/container/definition"        [:as req] (uic/view       conf req (a/container-definition conf req)))
  (GET "/ui/:mp/container/state/:idx"        [:as req] (uic/view-state conf req (a/container-state      conf req)))
  (GET "/ui/:mp/container/ctrl/:idx"         [:as req] (uic/view-ctrl  conf req (a/container-ctrl       conf req)))
  (GET "/ui/:mp/container/definition/:idx"   [:as req] (uic/view       conf req (a/container-definition conf req)))
    
  (GET "/ws"                                 [:as req] (ws/main  conf req))
  
  (route/resources "/")
  (route/not-found (res/response {:error "not found"})))

(def app
  (-> (handler/site app-routes)
      (middleware/wrap-json-body {:keywords? true})
      middleware/wrap-json-response))

(defn start-ws!
  [conf]
  (st/register! "*" "*" "*" "*"  (fn [msg]
                                   (when-let [k (st/msg->key msg)]
                                     (ws/send-to-ws-clients conf {:key (ui/make-selectable k) :value (st/key->val k)}))) "c"))
  
(defn stop-ws! [conf] (st/de-register! "*" "*" "*" "*"  "c"))

(defn init-log!
  [{conf :mulog }]
  (mu/set-global-context! {:app-name "cmp"})
  (mu/start-publisher! conf))

(defn stop []
  (stop-ws! conf)
  (run! (fn [mp-id]
          (mu/log ::stop :message "stop mpd" :mp-id mp-id)
          (a/m-stop conf mp-id))
        (:build-on-start conf))
  (when @server (@server :timeout 100)
        (mu/log ::stop :message "stop server")
        (reset! server nil)
        (mu/log ::stop :message "stop logger")
        (@logger)
        (reset! logger nil)))

(defn start []
  (reset! logger (init-log! conf))
  (mu/log ::start :message "start ws listener")
  (start-ws! conf)
  (mu/log ::start :message "refresh tasks")
  (a/t-refresh conf)
  (run! (fn [mp-id]
          (mu/log ::start :message "build mpd" :mp-id mp-id)
          (a/m-build-ltm conf mp-id))
        (:build-on-start conf))
  (mu/log ::start :message "start server")
  (reset! server (run-server #'app (:api conf))))


(defn -main [& args]
  (mu/log ::start :message "call -main")
  (start))
