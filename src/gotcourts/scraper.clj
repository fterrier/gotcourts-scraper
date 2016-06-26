(ns gotcourts.scraper
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [org.httpkit.client :as http]
            [clojure.string :as str]
            [cheshire.core :refer :all]
            [clojure.edn :as edn]))

(defprotocol Scrape
  (retrieve-raw-data [this params]))

(defn- retrieve-and-transform-data [call-fn url options response-fn]
  (let [response     (call-fn url options)
        return-value (response-fn @response)]
    return-value))

(defn- retrieve-cookie []
  (retrieve-and-transform-data 
   http/get "https://www.gotcourts.com/de/"
   {} (fn [response]
        (let [set-cookie (->> response
                              :headers
                              :set-cookie)]
          (first (str/split set-cookie #";"))))))
  
(defn- retrieve-apikey [cookie]
                 (retrieve-and-transform-data 

                  http/post "https://www.gotcourts.com/de/api2/public/login/web"
                  {:headers     {"Cookie" cookie}
                   :form-params {:username "test@test123.com" 
                                 :password "vivegotcourts"}}
                  (fn [response]
                    (->> response 
                         :body
                         (#(parse-string % true))
                         :response 
                         :apiKey))))
  
(defn- retrieve-data [cookie apikey {:keys [id date]}]
               (retrieve-and-transform-data 
                http/get 
                (str "https://www.gotcourts.com/de/api/secured/player/club/reservations/" id "?date=" date)
                {:headers {"Cookie"      cookie 
                           "X-GOTCOURTS" (str "ApiKey=\"" apikey "\"")}}
                (fn [response]
                  (->> response
                       :body
                       (#(parse-string % true))
                       :response))))

(defrecord GotCourts []
  Scrape
  (retrieve-raw-data [scraper params]
    (let [cookie (retrieve-cookie)
          apikey (retrieve-apikey cookie)
          data   (retrieve-data cookie apikey params)]
      data)))

(defn gotcourts-scraper []
  (->GotCourts))
