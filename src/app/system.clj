(ns app.system
  (:gen-class)
  (:require	[com.stuartsierra.component :as component]
             [app.server :as server]
             [app.scheduler :as scheduler]
             [gotcourts.handler :as handler]
             [gotcourts.scraper :as scraper]))

(defn system [config-options]
  (let [{:keys [port]} config-options]
    (component/system-map
      :scraper     (scraper/gotcourts-scraper)
      :routes      (component/using
                     (handler/map->ServerRoutes {}) [:scraper])
      :http-server (component/using
                     (server/map->HTTPServer {:port port}) [:routes])
      :scheduler   (component/using 
                     (scheduler/map->Scheduler {}) [:scraper]))))
