(ns gotcourts.alerter-test
  (:require [clojure.test :refer :all]
            [gotcourts
             [alerter :as alr]
             [task :as task]]))

(deftest test-alerter-courts
  (testing "No alerts when no free court"
    (is (= [] 
           (alr/get-alerts-for-courts
            [{:id 1 :filtered-free-slots []}]
            [{:id 1 :filtered-free-slots []}]))))

  (testing "No alert when new court but no slot"
    (is (= [] 
           (alr/get-alerts-for-courts
            []
            [{:id 1 :filtered-free-slots []}]))))

  (testing "Alert when new court"
    (is (= [[:new-slot {:slot :slot1 :id 1}]]
           (alr/get-alerts-for-courts
            []
            [{:id 1 :filtered-free-slots [:slot1]}]))))
  
  (testing "Alert when new slot"
    (is (= [[:new-slot {:slot :slot1 :id 1}]]
           (alr/get-alerts-for-courts
            [{:id 1 :filtered-free-slots []}]
            [{:id 1 :filtered-free-slots [:slot1]}]))))

  (testing "No alert when court gone but no slot"
    (is (= []
           (alr/get-alerts-for-courts
            [{:id 1 :filtered-free-slots []}]
            []))))

  (testing "Alert when court gone"
    (is (= [[:gone-slot {:slot :slot1 :id 1}]]
           (alr/get-alerts-for-courts
            [{:id 1 :filtered-free-slots [:slot1]}]
            []))))
  
  (testing "Alert when slot gone"
    (is (= [[:gone-slot {:slot :slot1 :id 1}]] 
           (alr/get-alerts-for-courts
            [{:id 1 :filtered-free-slots [:slot1]}]
            [{:id 1 :filtered-free-slots []}])))))

(deftest test-alerter
  (testing "Test alerts on same data produces empty alert data"
    (let [data   {17 {:name "Hardhof (Sportamt)" 
                      :courts
                      [{:id 86 :label "Platz 6 (WVZ)" 
                        :free-slots [{:startTime 50400 :endTime 64800}]
                        :filtered-free-slots [{:startTime 50400 :endTime 64800}]}]}}
          alerts (alr/get-alerts data data)]
      (is (= nil (get-in alerts [17 :alerts])))))
  
  (testing "Test alerts on new data"
    (let [data       {17 {:name "Hardhof (Sportamt)" 
                      :courts
                      [{:id 86 :label "Platz 6 (WVZ)" 
                        :free-slots [{:startTime 50400 :endTime 64800}]
                        :filtered-free-slots [{:startTime 50400 :endTime 64800}]}]}}  
          alerts     (alr/get-alerts nil data)]
      (is (= [[:new-slot {:slot {:startTime 50400, :endTime 64800}, :id 86}]] 
             (get-in alerts [17 :alerts]))))))
