(ns gotcourts.handler
  (:require [compojure
             [core :refer [context GET routes]]
             [route :as route]]
            [gotcourts.checker :as chk]
            [ring.middleware
             [json :refer [wrap-json-response]]
             [params :refer [wrap-params]]]))

(defn- free-slots [retrieve-data-fn id date transform-fn]
  (let [data @(retrieve-data-fn {:id id :date date})]
    (if (:error data) 
      {:status 400
       :headers {"Content-Type" "application/json; charset=utf-8"}
       :body (select-keys data [:error :message])}
      {:headers {"Content-Type" "application/json; charset=utf-8"}
       :body (transform-fn (chk/extract-data data))})))

(defn- app-routes [retrieve-data-fn]
  (routes
    ; format is yyyy-mm-dd
    (context "/gotcourts/:id{[0-9]+}/:date{[0-9]{4}-[0-9]{2}-[0-9]{2}}" [id date]
             (GET "/" []
                  (free-slots retrieve-data-fn id date identity))
             (GET "/courts" {:keys [params]} []
                  (free-slots retrieve-data-fn 
                              id date 
                              (fn [data] 
                                (let [courts (:courts data)]
                                  (chk/filter-free courts 
                                                   (read-string (:start params)) 
                                                   (read-string (:end params))))))))))

(defn app [retrieve-data-fn]
  "Takes as a parameter a function that returns a future that delivers when the
   data is there"
  (wrap-json-response (wrap-params (app-routes retrieve-data-fn))))
