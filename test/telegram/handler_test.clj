(ns telegram.handler-test
  (:require [cheshire.core :as json]
            [clojure.core.async :refer [<!! chan close!]]
            [clojure.test :refer [deftest is testing]]
            [telegram.handler :as handler]))

(defn request [resource web-app body]
  (web-app {:request-method :post :uri resource :body (json/generate-string body)}))

(defn- test-web-app [handler-fn]
  (handler/app "test-bot-id" nil handler-fn))

(deftest handler-test
  (testing "Test message handler receives request"
    (let [handler-atom (atom {})
          channel      (chan)
          handler-fn   (fn [body _]
                         (reset! handler-atom body)
                         (close! channel))
          response     (request "/telegram" 
                                (test-web-app handler-fn) 
                                {:test "123"})]
      (<!! channel)
      (is (= "ok" (:body response)))
      (is (= "{\"test\":\"123\"}" @handler-atom))))
  
  (testing "Faulty handler does not starve client"
    (let [handler-fn (fn [body channel] (throw (Exception.)))
          web-app    (test-web-app nil)]
      (doseq [_ (range 0 20)]
        (request "/telegram" web-app nil))
      (is true)))

  (testing "All messages are received in sequence"
    (let [handler-atom (atom [])
          channel      (chan)
          handler-fn   (fn [body _] 
                         (swap! handler-atom conj body)
                         (when (= "99" body) (close! channel)))
          web-app      (test-web-app handler-fn)]
      (doseq [x (range 0 100)]
        (request "/telegram" web-app x))
      (<!! channel)
      (is (= (into [] (map str (range 0 100))) @handler-atom)))))
