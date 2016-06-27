(ns gotcourts.alerter-test
  (:require [clojure.test :refer :all]
            [gotcourts.alerter :as alr]))

(deftest test-alerter
  (testing "No alerts when no free court"
    (is (= [] 
           (alr/get-alerts 
            {1 {:id 1 :filtered-free-slots []}}
            {1 {:id 1 :filtered-free-slots []}}))))

  (testing "No alert when new court but no slot"
    (is (= [] 
           (alr/get-alerts
            {}
            {1 {:id 1 :filtered-free-slots []}}))))

  (testing "Alert when new court"
    (is (= [[:new-slot {:slot :slot1 :id 1}]]
           (alr/get-alerts
            {}
            {1 {:id 1 :filtered-free-slots [:slot1]}}))))
  
  (testing "Alert when new slot"
    (is (= [[:new-slot {:slot :slot1 :id 1}]]
           (alr/get-alerts 
            {1 {:id 1 :filtered-free-slots []}}
            {1 {:id 1 :filtered-free-slots [:slot1]}}))))

  (testing "No alert when court gone but no slot"
    (is (= []
           (alr/get-alerts
            {1 {:id 1 :filtered-free-slots []}}
            {}))))

  (testing "Alert when court gone"
    (is (= [[:gone-slot {:slot :slot1 :id 1}]]
           (alr/get-alerts
            {1 {:id 1 :filtered-free-slots [:slot1]}}
            {}))))
  
  (testing "Alert when slot gone"
    (is (= [[:gone-slot {:slot :slot1 :id 1}]] 
           (alr/get-alerts
            {1 {:id 1 :filtered-free-slots [:slot1]}}
            {1 {:id 1 :filtered-free-slots []}})))))
