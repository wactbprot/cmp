(ns cmp.log
  (:require [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.3rd-party.gelf :as gelf]))

(def stdout-appender {:enabled?   true
                      :async?     false
                      :min-level  :info
                      :output-fn  :inherit
                      :fn (fn [data]
                            (let [{:keys [output_]} data
                                  formatted-output-str (force output_)]
                              (println formatted-output-str)))})

(defn init
  "Initializes the logging. Default appenders are println and gelf."
  ([]
   (init "127.0.0.1" 12201))
  ([host port]
   (timbre/with-config
    (timbre/merge-config!
     {:level :debug
      :appenders {:println stdout-appender
                  :gelf (gelf/gelf-appender host port :udp)}}))))

(defn stop-repl-out
  "Stops the println appender."
  []
  (timbre/with-config
    (timbre/merge-config!
     {:appenders {:println {:enabled? false}}})))

(defn start-repl-out
  "Starts the println appender."
  []
  (timbre/with-config
    (timbre/merge-config!
     {:appenders {:println stdout-appender}})))
