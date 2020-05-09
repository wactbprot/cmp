(defproject cmp "0.8.1"
  :description "A study of an interpreter for measurement 
  program (mp) definitions (mpd) written in clojure."
  :url "https://github.com/wactbprot/cmp"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/core.async "0.4.490"]
                 [com.taoensso/carmine "2.19.1"]
                 [com.ashafa/clutch "0.4.0"]
                 [clj-time "0.15.0"]
                 [org.clojure/data.json "0.2.6"]
                 [com.taoensso/timbre "4.10.0"]
                 [aero "1.1.3"]
                 [clj-http "3.10.0"]
                 [biz.paluch.logging/logstash-gelf "1.12.0"]
                 [ch.qos.logback/logback-classic "1.0.1"]
                 [org.graylog2/gelfclient "1.4.1"]
                 [clojang/codox-theme "0.2.0-SNAPSHOT"]
                 ]
  :main ^:skip-aot cmp.core
  :target-path "target/%s"
  :plugins [[lein-cloverage "1.1.2"]
            [lein-codox "0.10.7"]
            [ns-graph "0.1.3"]
            [jonase/eastwood "0.3.7"]
	    ]
  :cloverage {:low-watermark 30
             :high-watermark 60}
  ;;  :codox {:themes [:rdash]
  :codox {:themes [:clojang]
          :metadata {:doc/format :markdown}
          :source-uri "https://github.com/wactbprot/cmp/blob/master/{filepath}#L{line}"}
  :ns-graph {:name "cmp"
             :abbrev-ns false
             :source-paths ["src/"]
             :exclude ["java.*" "clojure.*" "taoensso.timbre"]}
  ;; :profiles {:uberjar {:aot :all}}
  )
