(ns gotcourts.scraper
  (:require [cats.monad.maybe :as maybe]
            [cats.core :as m]
            [cheshire.core :refer :all]
            [clojure.string :as str]
            [org.httpkit.client :as http]
            [clojure.tools.logging :as log]))

(defprotocol Scrape
  (retrieve-raw-data [this params] "Returns a promise that delivers when the data is scraped"))

(defn- retrieve-and-transform-data [call-fn url options response-fn]
  (log/debug "Calling gotcourts" url options)
  (let [response @(call-fn url options)]
    (log/debug "Got response" (:status response) (:error response))
    (if (or (:error response) (not= 200 (:status response)))
      (maybe/nothing)
      (maybe/just (response-fn response)))))

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
    (log/info "Scraping gotcourts with params" params)
    (let [p (promise)]
      (future
        (deliver p
                 @(m/mlet [cookie (retrieve-cookie)
                           apikey (retrieve-apikey cookie)
                           data   (retrieve-data cookie apikey params)]
                          (m/return data))))
      p)))

(defn gotcourts-scraper []
  (->GotCourts))

(comment
  @(retrieve-raw-data (gotcourts-scraper) {:id "21" :date "2015-07-20"}))

