(defproject cmp "0.1.1"
  :description "A study of an interpreter for measurement program definitions (mpd) written in clojure."
  :url "https://github.com/wactbprot/cmp"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/core.async "0.4.490"]
                 [com.taoensso/carmine "2.19.1"]
                 [com.ashafa/clutch "0.4.0"]
                 [clj-time "0.15.0"]
                 [io.aviso/pretty "0.1.37"]
                 [org.clojure/data.json "0.2.6"]
                 [com.taoensso/timbre "4.10.0"]
                 [aero "1.1.3"]
                 [biz.paluch.logging/logstash-gelf "1.12.0"]
                 [ch.qos.logback/logback-classic "1.0.1"]
                 [org.graylog2/gelfclient "1.4.1"]]
  :main ^:skip-aot cmp.core
  :target-path "target/%s"
  :repl-options {
                 :prompt (fn [ns] (str "You are hacking in " ns "=> " ))
                 :welcome (println "Its  REPL time!")}
  :profiles {:uberjar {:aot :all}})
