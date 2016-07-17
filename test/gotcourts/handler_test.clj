(ns gotcourts.handler-test
  (:require [cheshire.core :as json]
            [clojure
             [edn :as edn]
             [test :refer [deftest is testing]]]
            [gotcourts.handler :as handler]))

(defn request [resource web-app & params]
  (web-app {:request-method :get :uri resource :params (first params)}))

(defn- gen-response [status-code body]
  {:status status-code
   :headers {"Content-Type" "application/json; charset=utf-8"}
   :body body})

(defn- test-web-app 
  ([]
   (handler/app (fn [params] (future))))
  ([fixture]
   (handler/app (fn [params] (future (edn/read-string (slurp fixture)))))))

(deftest app-handler-test
  ;; (testing "Not found"
  ;;   (is (= (gen-response 404 "{\"message\":\"not found\"}")
  ;;          (request "/not-found" (test-web-app)))))

  (testing "Get all courts and free slots"
    (let [response (request "/gotcourts/17/2015-12-12/" (test-web-app "fixtures/hardhof.edn"))
          parsed-response (json/parse-string (:body response) true)]
      (is (= "Hardhof (Sportamt)" (:name parsed-response)))
      (is (= 11 (count (:courts parsed-response))))))
  
  (testing "Filter free slots"
    (let [response (request "/gotcourts/17/2015-12-12/courts" 
                            (test-web-app "fixtures/hardhof.edn")
                            {:start "50400" :end "64800"})
          parsed-response (json/parse-string (:body response) true)]
      (is (= 8 (count parsed-response)))))

  (testing "Wrong params"))

