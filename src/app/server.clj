(ns app.server
  (:require [ring.adapter.jetty :as ring]
            [com.stuartsierra.component :as component]
            [gotcourts.handler :as handler]))


(defrecord HTTPServer [port server routes]
  component/Lifecycle
  (start [component]
         (println ";; Starting HTTP server")
         (let [server (ring/run-jetty (:routes routes) {:port port
                                                        :join? false})]
           (assoc component :server server)))
  (stop [component]
        (println ";; Stopping HTTP server")
        (.stop server)
        (dissoc component :server)))
