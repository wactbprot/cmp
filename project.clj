(defproject cmp "0.1.1"
  :description "A study of an interpreter for measurement program definitions (mpd) written in clojure."
  :url "https://github.com/wactbprot/cmp"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/core.async "0.4.490"]
                 [com.taoensso/carmine "2.19.1"]
                 [com.ashafa/clutch "0.4.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [clj-time "0.15.0"]
                 [io.aviso/pretty "0.1.37"]
                 [org.clojure/data.json "0.2.6"]]
  :main ^:skip-aot cmp.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
