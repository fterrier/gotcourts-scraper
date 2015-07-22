(ns gotcourts.core-test
  (:require [com.stuartsierra.component :as component]
            [clojure.test :refer :all]
            [gotcourts.scraper :as scraper]
            [app.system :as system]
            [ring.mock.request :as mock]
            [clojure.edn :as edn]
            [midje.sweet :refer :all]
            [cheshire.core :refer :all]))

(def core-fixtures
  {:hardhof (edn/read-string (slurp "fixtures/hardhof.edn"))
   :lengg   (edn/read-string (slurp "fixtures/lengg.edn"))})

(defrecord StubScrape []
  scraper/Scrape
  (retrieve-cookie [_] "cookie")
  (retrieve-apikey [_ cookie] "apikey")
  (retrieve-data [_ cookie apikey {:keys [id date]}]
                 (:hardhof core-fixtures)))

(defn test-system []
  (assoc (system/system {:port 9999}) :scraper (->StubScrape)))

(defchecker json-contains [& expected]
  (checker [actual]
           (every? (fn [v] 
                     (= (get (parse-string actual true) (first v)) (second v))) 
                   (first expected))))

(deftest a-test
  (let [system (component/start (test-system))
        routes (:routes (:routes system))]
    (try
      (fact "test not found"
            (routes (mock/request :get "/")) => 
            (contains {:status  404
                       :body    "not found"}))
      (fact "test name"
            (routes (mock/request :get "/gotcourts/17/2017-05-12")) => 
            (contains {:body (json-contains {:name "Hardhof (Sportamt)"})}))
      (finally (component/stop system)))))
