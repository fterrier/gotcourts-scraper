(defproject gotcourts "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [http-kit "2.1.19"]
                 [cheshire "5.6.2"]
                 [compojure "1.5.1"]
                 [ring/ring-defaults "0.2.1"]
                 [ring/ring-jetty-adapter "1.5.0"]
                 [ring/ring-json "0.4.0"]
                 [mount "0.1.10"]
                 [jarohen/chime "0.1.9"]
                 [org.clojure/tools.logging "0.3.1"]
                 [funcool/cats "1.2.1"]
                 [wit/duckling "0.4.8"]
                 [org.clojure/tools.cli "0.3.5"]]
  :main ^:skip-aot app.main
  :uberjar-name "gotcourts.jar"
  :target-path "target/%s"
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.11"]]
                   :source-paths ["env/dev/src"]}
             :uberjar {:aot :all}})
