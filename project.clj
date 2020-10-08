(defproject cmp "0.14.0"
  :description "A study of an interpreter for measurement 
  program definitions (mpd) written in clojure."
  :url "https://github.com/wactbprot/cmp"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [com.taoensso/carmine "3.0.0"]
                 [com.ashafa/clutch "0.4.0"]
                 [clj-time "0.15.0"]
                 [org.clojure/data.json "0.2.6"]
                 [com.taoensso/timbre "5.0.0"]
                 [clj-http "3.10.0"]
                 [clojang/codox-theme "0.2.0-SNAPSHOT"]
                 ]
  :main ^:skip-aot cmp.core
  :target-path "target/%s"
  :plugins [[lein-cloverage "1.1.2"]
            [lein-codox "0.10.7"]
            [ns-graph "0.1.3"]
            [jonase/eastwood "0.3.7"]
	    [lein-marginalia "0.9.1"]]
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
