(ns app.server
  (:require [app.scraper :refer [scraper]]
            [clojure.core.async :refer [>!!]]
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

(defn start-www [{:keys [port]} scraper]
  (-> (routes 
       (gotcourts-handler/app scraper)
       ;; TODO fix this
       (telegram-handler/app "244753532:AAEm7RIud0nszMYOWepyE5lJpQwVt8bm1D8" 
                             "73ed7de4.ngrok.io"
                             (fn [_ ch] (>!! ch "test")))
       (route/not-found not-found))
      (wrap-defaults api-defaults)
      (run-jetty {:join? false
                  :port port})))

;; TODO hook config
(defstate web-server 
  :start (start-www {:port 8000} scraper)
  :stop (.stop web-server))
