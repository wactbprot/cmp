(ns cmp.log
  (:require [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.3rd-party.gelf :as gelf]))

(def stdout-appender {:enabled?   true
                      :async?     false
                      :min-level  :debug
                      :output-fn  :inherit
                      :fn (fn [data]
                            (let [{:keys [output_]} data
                                  formatted-output-str (force output_)]
                              (println formatted-output-str)))})

(defn start-gelf
  "Initializes the logging to gelf."
  ([]
   (start-gelf "127.0.0.1" 12201))
  ([host port]
   (timbre/with-config
    (timbre/merge-config!
     {:level :debug
      :appenders {:gelf (gelf/gelf-appender host port :udp)}}))))

(defn stop-gelf
  "Stops the gelf appender."
  []
  (timbre/with-config
    (timbre/merge-config!
     {:appenders {:gelf {:enabled? false}}})))

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
