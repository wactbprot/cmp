(ns cmp.server
    ^{:author "wactbprot"
      :doc "Provides a REST api for cmp info and ctrl."}
  (:require [compojure.route          :as route]
            [com.brunobonacci.mulog   :as mu]
            [cmp.config               :as c]
            [cmp.api                  :as a]
            [cmp.ui.listener          :as uil]
            [cmp.ui.container         :as uic]
            [cmp.ui.mp-meta           :as uim]
            [cmp.ui.ws                :as ws]
            [cmp.st-mem               :as st] 
            [compojure.core           :refer :all]
            [compojure.handler        :as handler]
            [org.httpkit.server       :refer [run-server]]
            [ring.middleware.json     :as middleware]
            [ring.util.response       :as res]
            ))

(def conf (c/config))

(defonce server (atom nil))

(defonce logger (atom nil))

(defroutes app-routes
  (GET "/config"                      []           (res/response conf))
  (GET "/listeners"                   [:as req]    (res/response (a/listeners conf req)))
  (GET "/tasks"                       [:as req]    (res/response (a/tasks     conf req)))
  
  (GET "/:mp/meta"                    [mp :as req] (res/response (a/mp-meta   conf req mp)))
  (GET "/:mp/container/title"         [mp :as req] (res/response (a/container-title conf req mp)))

  (GET "/ui/listeners"                   [:as req] (uil/view conf (a/listeners conf req)))

  (GET "/ui/:mp/meta"                 [mp :as req] (uim/view conf
                                                             (a/mp-meta conf req mp)))
  (GET "/ui/:mp/container/title"      [mp :as req] (uic/view conf
                                                             (a/container-title conf req mp)))
  (GET "/ui/:mp/container/descr"      [mp :as req] (uic/view conf
                                                             (a/container-descr conf req mp)))
  (GET "/ui/:mp/container/ctrl"       [mp :as req] (uic/view conf
                                                             (a/container-ctrl conf req mp)))
  (GET "/ui/:mp/container/state"      [mp :as req] (uic/view conf
                                                             (a/container-state conf req mp)))
  (GET "/ui/:mp/container/definition" [mp :as req] (uic/view conf
                                                             (a/container-definition conf req mp)))

  (GET "/ws"                     [:as req] (ws/main  conf req))
  
  (route/resources "/")
  (route/not-found (res/response {:error "not found"})))

(def app
  (-> (handler/site app-routes)
      (middleware/wrap-json-body {:keywords? true})
      middleware/wrap-json-response))

(defn init-ws!
  [conf]
  (st/register! "*" "*" "*" "*"  (fn [msg]
                                   (when (st/msg->key msg)
                                     (prn msg)
                                     (ws/send-to-ws-clients conf msg)))) "c")
  

(defn init-log!
  [{conf :mulog }]
  (mu/set-global-context! {:app-name "cmp"})
  (mu/start-publisher! conf))

(defn stop []
  (when @server (@server :timeout 100)
        (@logger)
        (reset! logger nil)
        (reset! server nil)))

(defn start []
  (init-ws! conf)
  (reset! logger (init-log! conf))
  (mu/log ::start :message "start cmp rest api")
  (reset! server (run-server #'app (:api conf))))


(defn -main [& args]
  (mu/log ::start :message "call -main")
  (start))
