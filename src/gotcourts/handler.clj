(ns gotcourts.handler
  (:require [com.stuartsierra.component :as component]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults secure-site-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.middleware.params :refer [wrap-params]]
            [gotcourts.scraper :refer :all]
            [gotcourts.checker :refer :all]))

(defn free-slots [scraper id date transform-fn]
  ; TODO cache this data and deal with error on cookie, apikey
  (let [data (retrieve-raw-data scraper id date)]
    (if (:error data) 
      {:status 400
       :headers {"Content-Type" "application/json; charset=utf-8"}
       :body (select-keys data [:error :message])}
      {:headers {"Content-Type" "application/json; charset=utf-8"}
       :body (transform-fn (extract-data data))})))

(defn not-found [_] 
  {:status 404
   :body   "not found"})

(defn app-routes []
  (compojure.core/routes
    ; format is yyyy-mm-dd
    (context "/gotcourts/:id{[0-9]+}/:date{[0-9]{4}-[0-9]{2}-[0-9]{2}}" [id date]
             (defroutes court-routes
               (GET "/" {:keys [scraper]} 
                    (free-slots scraper id date identity))
               (GET "/courts" {:keys [params scraper]} []
                    (free-slots scraper id date (fn [data] 
                                                  (let [courts (:courts data)]
                                                    (filter-free courts 
                                                                 (read-string (:start params)) 
                                                                 (read-string (:end params)))))))))
    (route/not-found not-found)))

(defn app []
  (wrap-json-response (wrap-params (app-routes))))

;; this below is from :
;; https://github.com/valichek/component-compojure
;; 
(defn wrap-components [handler deps]
  (fn [req]
    (println req)
    (handler (merge req deps))))

(defn make-handler [routes deps]
  (-> routes
      (wrap-components deps)))

(defrecord ServerRoutes [scraper]
  component/Lifecycle
  (start [component]
         (println ";; Starting server routes")
         (assoc component 
           :routes (make-handler (app) {:scraper scraper})))
  (stop [component] 
        (println ";; Stopping server routes")
        (dissoc component :routes)))

