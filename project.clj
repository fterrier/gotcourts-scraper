(defproject gotcourts "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [http-kit "2.1.18"]
                 [cheshire "5.5.0"]
                 [compojure "1.3.4"]
                 [ring/ring-defaults "0.1.2"]
                 [ring/ring-jetty-adapter "1.2.1"]
                 [ring/ring-json "0.3.1"]]
  ; :main ^:skip-aot gotcourts.core
  :uberjar-name "gotcourts.jar"
  :target-path "target/%s"
  :profiles {:dev {:source-paths ["env/dev/src"]
                   :plugins [[lein-ring "0.8.13"]]
                   :ring {:handler gotcourts.handler/app}}
             :uberjar {:aot :all}})
