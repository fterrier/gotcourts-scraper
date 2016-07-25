(ns gotcourts.task-test
  (:require [clj-time
             [core :as t]
             [format :as f]]
            [clojure
             [edn :as edn]
             [test :refer [deftest is testing]]]
            [gotcourts.task :as task]))

(defn mock-scraper [fixture-map]
  (fn [{:keys [id]}]
    (future (edn/read-string (slurp (get fixture-map id))))))

(defn mock-notifier [alert-atom]
  (fn [alert-data]
    (reset! alert-atom alert-data)))

(deftest extract-data-date-test
  (testing "Extract function uses right date"
    (let [test-date 
          (f/parse (f/formatter "dd-MM-yyyy" (t/default-time-zone)) "27-11-2015")]
      (task/extract-gotcourts
       (fn [{:keys [date]}] 
         (is (= "2015-11-27" date))
         (future {}))
       [1] test-date 50000 60000))))

(deftest extract-data-test
  (testing "Extract function generates filtered-court data for single court"
    (let [data (task/extract-gotcourts 
                (mock-scraper {17 "fixtures/hardhof.edn"})
                [17] (t/now) 50400 64800)]
      (is (= 8 (count 
                (get-in data [17 :courts]))))))
  
  (testing "Extract function generates filtered-court data for 2 courts"
    (let [data (task/extract-gotcourts
                (mock-scraper {17 "fixtures/hardhof.edn"
                               16 "fixtures/lengg.edn"})
                [17 16] (t/now) 50400 60000)]
      (is (= 9 (count (get-in data [17 :courts]))))
      (is (= 4 (count (get-in data [16 :courts])))))))

(deftest handle-alerts-test
  (testing "Test alerts on same data produces empty alert data"
    (let [data       (task/extract-gotcourts 
                      (mock-scraper {17 "fixtures/hardhof.edn"})
                      [17] (t/now) 50400 64800)
          alerts     (task/get-alerts data data)]
      (is (= nil (get-in alerts [17 :alerts])))))
  
  (testing "Test alerts on new data"
    (let [data       (task/extract-gotcourts 
                      (mock-scraper {17 "fixtures/hardhof.edn"})
                      [17] (t/now) 50400 64800)
          alerts     (task/get-alerts nil data)]
      (println data)
      (is (= [[:new-slot {:slot {:startTime 50400, :endTime 64800}, :id 86}]
              [:new-slot {:slot {:startTime 50400, :endTime 64800}, :id 88}]
              [:new-slot {:slot {:startTime 43200, :endTime 61200}, :id 91}]
              [:new-slot {:slot {:startTime 54000, :endTime 64800}, :id 90}]
              [:new-slot {:slot {:startTime 50400, :endTime 57600}, :id 82}]
              [:new-slot {:slot {:startTime 46800, :endTime 61200}, :id 83}]
              [:new-slot {:slot {:startTime 50400, :endTime 61200}, :id 87}]
              [:new-slot {:slot {:startTime 50400, :endTime 61200}, :id 84}]] 
             (get-in alerts [17 :alerts]))))))

