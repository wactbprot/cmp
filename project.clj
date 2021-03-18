(defproject cmp "0.22.0"
  :description "A study of an interpreter for measurement 
  program definitions (mpd) written in clojure."
  :url "https://github.com/wactbprot/cmp"
  :dependencies [[org.clojure/clojure                  "1.10.2"]
                 [com.taoensso/carmine                 "3.0.0"]
                 [com.ashafa/clutch                    "0.4.0"]
                 [org.clojars.wactbprot/vl-data-insert "0.2.1"]
                 [cheshire                             "5.10.0"]
                 [clj-time                             "0.15.0"]
                 [compojure                            "1.6.1"]
                 [http-kit                             "2.5.0"]
                 [hiccup                               "1.0.5"]
                 [ring/ring-defaults                   "0.3.2"]
                 [ring/ring-core                       "1.7.1"]
                 [ring/ring-devel                      "1.7.1"]
                 [ring/ring-json                       "0.5.0"]
                 [ring/ring-codec                      "1.1.3"]
                 [com.brunobonacci/mulog               "0.6.0"]
                 [com.brunobonacci/mulog-elasticsearch "0.6.0"]
                 [djblue/portal                        "0.9.0"]
                 [clj-http                             "3.10.0"]]
  :repl-options {:init-ns cmp.server}
  :plugins [[lein-cloverage       "1.1.2"]
            [lein-codox           "0.10.7"]
	    [lein-marginalia      "0.9.1"]]
  :cloverage {:low-watermark 30
              :high-watermark 60}
  :codox {:metadata {:doc/format :markdown}
          :source-uri "https://github.com/wactbprot/cmp/blob/master/{filepath}#L{line}"}
  :resource-paths ["resources"]
  :repositories   [["repsy" {:url "https://repo.repsy.io/mvn/wactbprot/cmp"
                             :sign-releases false}]]
  :main cmp.server
  :aot [cmp.server]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
