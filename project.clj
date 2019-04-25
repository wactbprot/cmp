(defproject cmp "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [com.taoensso/carmine "2.19.1"]
                 [com.ashafa/clutch "0.4.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [org.clojure/data.json "0.2.6"]]
  :main ^:skip-aot cmp.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
