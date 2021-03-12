(ns cmp.server
    ^{:author "wactbprot"
      :doc "Provides a server for cmp info and ctrl. Starts up the configured mpds."}
  (:require [compojure.route          :as route]
            [com.brunobonacci.mulog   :as mu]
            [cmp.config               :as config] 
            [cmp.cli                  :as cli]
            [cmp.api                  :as a]
            [cmp.ui.index             :as ui]
            [cmp.ui.listener          :as uil]
            [cmp.ui.container         :as uic]
            [cmp.ui.mp-meta           :as uim]
            [cmp.ui.setup             :as uis]
            [cmp.ui.ws                :as ws]
            [cmp.st-mem               :as st]
            [compojure.core           :refer :all]
            [compojure.handler        :as handler]
            [org.httpkit.server       :refer [run-server]]
            [ring.middleware.json     :as middleware]
            [ring.util.response       :as res])
    (:use   [clojure.repl])
    (:gen-class))

(def conf (config/config))

(defonce server (atom nil))

(declare restart)

(defroutes app-routes
  (GET "/ui/setup"                              [:as req] (uis/view conf req (a/listeners conf req)))
  (GET "/ui/listeners"                          [:as req] (uil/view conf req (a/listeners conf req)))
  (GET "/ui"                                    [:as req] (uil/view conf req (a/listeners conf req)))
  (GET "/ui/:mp/meta"                           [:as req] (uim/view conf req (a/mp-meta conf req)))
  (GET "/ui/:mp"                                [:as req] (uim/view conf req (a/mp-meta conf req)))
  (GET "/ui/:mp/container/ctrl"                 [:as req] (uic/view-ctrl  conf req (a/container-ctrl       conf req)))
  (GET "/ui/:mp/container/state"                [:as req] (uic/view-state conf req (a/container-state      conf req)))
  (GET "/ui/:mp/container/definition"           [:as req] (uic/view       conf req (a/container-definition conf req)))
  (GET "/ui/:mp/container/ctrl/:idx"            [:as req] (uic/view-ctrl  conf req (a/container-ctrl       conf req)))
  (GET "/ui/:mp/container/state/:idx"           [:as req] (uic/view-state conf req (a/container-state      conf req)))
  (GET "/ui/:mp/container/definition/:idx"      [:as req] (uic/view       conf req (a/container-definition conf req)))
  (GET "/ui/:mp/container/state/:idx/:seq"      [:as req] (uic/view-state conf req (a/container-state      conf req)))
  (GET "/ui/:mp/container/definition/:idx/:seq" [:as req] (uic/view       conf req (a/container-definition conf req)))
  (POST "/:mp/container"                        [:as req] (res/response (a/set-val! conf req)))
  (POST "/cmd"                                  [:as req] (res/response
                                                           (condp = (a/cmd conf req)
                                                             {:restart "server"} ((fn [](future (restart))
                                                                                    {:ok true}))
                                                             {:rebuild "tasks"}  ((fn [] (future (cli/t-refresh conf))
                                                                                    {:ok true}))
                                                             {:nil (prn (a/cmd conf req))})))
  (GET "/ws"                                    [:as req] (ws/main        conf req))
  (route/resources "/")
  (route/not-found (res/response {:error "not found"})))

(def app
  (-> (handler/site app-routes)
      (middleware/wrap-json-body {:keywords? true})
      middleware/wrap-json-response))

(defn stop []
  (mu/log ::stop :message "stop ui web socket listener")
  (ws/stop! conf)
  (run! (fn [mp-id]
          (mu/log ::stop :message "stop mpd" :mp-id mp-id)
          (cli/m-stop conf mp-id))
        (:build-on-start conf))
  (when @server (@server :timeout 100)
        (mu/log ::stop :message "stop server")
        (reset! server nil)
        (mu/log ::stop :message "stop logger")
        (cli/stop-log! conf))
  {:ok true})

(defn start []
  (cli/start-log! conf)
  (mu/log ::start :message "start ui web socket listener")
  (ws/start! conf)
  (mu/log ::start :message "refresh tasks")
  (cli/t-refresh conf)
  (run! (fn [mp-id]
          (mu/log ::start :message "build mpd" :mp-id mp-id)
          (cli/m-build-ltm conf mp-id))
        (config/build-on-start conf))
  (mu/log ::start :message "start server")
  (reset! server (run-server #'app (:api conf)))
  {:ok true})

(defn restart []
  (Thread/sleep 100)
  (stop)
  (Thread/sleep 1000)
  (start))

(defn -main [& args]
  (mu/log ::main :message "call -main")
  (start))
