(ns app.server
  (:require [app
             [scheduler :as scheduler]
             [scraper :refer [scraper]]]
            [compojure
             [core :refer [routes]]
             [route :as route]]
            [gotcourts.handler :as gotcourts-handler]
            [gotcourts.bot :as bot]
            [mount.core :refer [defstate]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.defaults :refer [api-defaults wrap-defaults]]
            [telegram.handler :as telegram-handler]))

(defn- not-found [_] 
  {:status 404
   :body   {:message "not found"}})

(defn start-www [{:keys [port]} scraper]
  (-> (routes 
       (gotcourts-handler/app scraper)
       (telegram-handler/app 
        ;; TODO use a configuration
        "244753532:AAEm7RIud0nszMYOWepyE5lJpQwVt8bm1D8" "10d35f42.ngrok.io"
        (bot/create-bot scheduler/scheduler))
       (route/not-found not-found))
      (wrap-defaults api-defaults)
      (run-jetty {:join? false
                  :port port})))

;; TODO hook config
(defstate web-server 
  :start (start-www {:port 8000} scraper)
  :stop (.stop web-server))
