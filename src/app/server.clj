(ns app.server
  (:require [gotcourts.handler :as handler]
            [app.scraper :refer [scraper]]
            [mount.core :refer [defstate]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.defaults :refer [api-defaults wrap-defaults]]))

(defn start-www [{:keys [port]} scraper]
  (-> (handler/app scraper)
      (wrap-defaults api-defaults)
      (run-jetty {:join? false
                  :port port})))

;; TODO hook config
(defstate web-server :start (start-www {:port 8000} scraper)
                     :stop (.stop web-server))
