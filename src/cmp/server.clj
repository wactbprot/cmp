(ns cmp.server
    ^{:author "wactbprot"
      :doc "Provides a REST api for cmp info and ctrl."}
  (:require [compojure.route          :as route]
            [com.brunobonacci.mulog   :as mu]
            [cmp.config               :as c]
            [cmp.api                  :as a]
            [cmp.ui.listener          :as uil]
            [cmp.ui.ws                :as ws]
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
  (GET "/config"                 []        (res/response conf))
  (GET "/listeners"              [:as req] (res/response (a/listeners conf req)))
  (GET "/ui/listeners"           [:as req] (res/response (uil/view conf (a/listeners conf req))))
  (GET "/tasks"                  [:as req] (res/response (a/tasks     conf req)))
  (GET "/:mp-id/container/title" [mp-id :as req] (res/response (a/container-title conf req mp-id)))

  (GET "/ws"                     [:as req] (ws/main  conf req))  

  (route/not-found (res/response {:error "not found"})))

(def app
  (-> (handler/site app-routes)
      (middleware/wrap-json-body {:keywords? true})
      middleware/wrap-json-response))

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
  (reset! logger (init-log! conf))
  (mu/log ::start :message "start cmp rest api")
  (reset! server (run-server app (:api conf))))


(defn -main [& args]
  (mu/log ::start :message "call -main")
  (start))
