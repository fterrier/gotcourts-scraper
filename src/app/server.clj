(ns app.server
  (:require [app
             [scheduler :as scheduler]
             [scraper :as scraper]]
            [compojure
             [core :refer [routes]]
             [route :as route]]
            [gotcourts.handler :as gotcourts-handler]
            [gotcourts.bot :as bot]
            [scheduler.scheduler :as task-scheduler]
            [mount.core :refer [defstate]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.defaults :refer [api-defaults wrap-defaults]]
            [telegram.handler :as telegram-handler]
            [gotcourts.task :as task]))

(defn- not-found [_] 
  {:status 404
   :body   {:message "not found"}})

(defn start-www [{:keys [port]}]
  (-> (routes 
       (gotcourts-handler/app scraper/scraper)
       (telegram-handler/app 
        ;; TODO use a configuration
        "244753532:AAEm7RIud0nszMYOWepyE5lJpQwVt8bm1D8" "10d35f42.ngrok.io"
        (let [schedule-fn (fn [interval task-fn] 
                            (task-scheduler/add-chime scheduler/scheduler 
                                                 interval task-fn))
              create-task-fn (task/create-gotcourts-task-creator scraper/scraper)]
          (bot/create-bot schedule-fn create-task-fn)))
       (route/not-found not-found))
      (wrap-defaults api-defaults)
      (run-jetty {:join? false
                  :port port})))

;; TODO hook config
(defstate web-server 
  :start (start-www {:port 8000})
  :stop (.stop web-server))
