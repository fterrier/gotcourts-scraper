(ns gotcourts.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults secure-site-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.middleware.params :refer [wrap-params]]
            [gotcourts.core :refer :all]
            [gotcourts.checker :refer :all]))

(defn free-slots [id date transform-fn]
  ; TODO cache this data and deal with error on cookie, apikey
  (let [cookie (homepage-cookie)
        apikey (login-apikey cookie)
        data   (court-data cookie apikey id date)]
    (println "cookie: " cookie " apikey: " apikey)
    (if (:error data) 
      {:status 400
       :headers {"Content-Type" "application/json; charset=utf-8"}
       :body (select-keys data [:error :message])}
      {:headers {"Content-Type" "application/json; charset=utf-8"}
       :body (transform-fn (extract-data data))})))

(defroutes api-routes
  (wrap-routes 
    ; format is yyyy-mm-dd
    (context "/gotcourts/:id{[0-9]+}/:date{[0-9]{4}-[0-9]{2}-[0-9]{2}}" [id date]
             (defroutes court-routes
               (GET "/" [] (free-slots id date identity))
               (wrap-params (GET "/courts" {params :params} []
                                 (free-slots id date (fn [data] 
                                                       (let [courts (:courts data)]
                                                         (filter-free courts 
                                                                      (read-string (:start params)) 
                                                                      (read-string (:end params))))))))))
    wrap-json-response)
  (route/not-found "404"))

(def app
  (wrap-routes api-routes wrap-defaults api-defaults))
