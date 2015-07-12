(ns gotcourts.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults secure-site-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-response]]
            [gotcourts.core :refer :all]))

(defroutes api-routes
  (wrap-routes (GET "/gotcourts/:id/:date" [id date] (free-slots id date)) wrap-json-response)
  (route/not-found "404"))

(def app
  (wrap-routes api-routes wrap-defaults api-defaults))