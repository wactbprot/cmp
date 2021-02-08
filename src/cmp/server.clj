(ns cmp.server
    ^{:author "wactbprot"
      :doc "Provides a REST api for cmp info and ctrl."}
  (:require [compojure.route          :as route]
            [com.brunobonacci.mulog   :as mu]
            [cmp.config               :as c]
            [cmp.api                  :as a]
            [compojure.core           :refer :all]
            [compojure.handler        :as handler]
            [org.httpkit.server       :refer [run-server]]
            [ring.middleware.json     :as middleware]))

(def conf (c/config))

(defonce server (atom nil))

(defonce logger (atom nil))

(defroutes app-routes
  (GET "/listeners"          [:as req]     (a/listeners conf req))
  (route/not-found (v/not-found)))

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
  (pp/pprint conf)
  (mu/log ::start :message "start cmp rest api")
  (reset! server (run-server app {:port 8010})))


(defn -main [& args]
  (mu/log ::start :message "call -main")
  (start))
