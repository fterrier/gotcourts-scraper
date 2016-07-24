(ns app.server
  (:require [app.scraper :as scraper]
            [app.bot :as bot]
            [compojure
             [core :refer [routes]]
             [route :as route]]
            [gotcourts.handler :as gotcourts-handler]
            [mount.core :refer [defstate]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.defaults :refer [api-defaults wrap-defaults]]
            [telegram.handler :as telegram-handler]))

(defn- not-found [_] 
  {:status 404
   :body   {:message "not found"}})

(defn start-www [{:keys [port]}]
  (-> (routes 
       (gotcourts-handler/app scraper/scraper)
       (telegram-handler/app 
        ;; TODO use a configuration
        "244753532:AAEm7RIud0nszMYOWepyE5lJpQwVt8bm1D8" "403429f6.ngrok.io" bot/bot)
       (route/not-found not-found))
      (wrap-defaults api-defaults)
      (run-jetty {:join? false
                  :port port})))

;; TODO hook config
(defstate web-server 
  :start (start-www {:port 8000})
  :stop (.stop web-server))
