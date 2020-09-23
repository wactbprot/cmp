(ns cmp.log
  (:require [taoensso.timbre :as timbre]))

(def stdout-appender {:enabled?   true
                      :async?     false
                      :min-level  :debug
                      :output-fn  :inherit
                      :fn (fn [data]
                            (let [{:keys [output_]} data
                                  formatted-output-str (force output_)]
                              (println formatted-output-str)))})

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
