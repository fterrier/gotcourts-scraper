(ns app.server
  (:require [app
             [bot :as bot]
             [config :as config]
             [scraper :as scraper]]
            [compojure
             [core :refer [routes]]
             [route :as route]]
            [gotcourts
             [handler :as gotcourts-handler]
             [scraper :as gotcourts-scraper]]
            [mount.core :as mount :refer [defstate]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.defaults :refer [api-defaults wrap-defaults]]
            [telegram.handler :as telegram-handler]))

(defn- not-found [_] 
  {:status 404
   :body   {:message "not found"}})

(defn start-www [{:keys [port]}]
  (println "starting on port" port)
  (let [{:keys [telegram]} config/config
        telegram-key       (:key telegram)
        telegram-host      (:host telegram)]
      (-> (routes 
           (gotcourts-handler/app 
            (partial scraper/scraper gotcourts-scraper/fetch-availabilities))
           (telegram-handler/app telegram-key telegram-host bot/bot)
           (route/not-found not-found))
          (wrap-defaults api-defaults)
          (run-jetty {:join? false :port port}))))

(defstate web-server
  :start (start-www (mount/args))
  :stop (.stop web-server))
