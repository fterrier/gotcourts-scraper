(ns gotcourts.task-test
  (:require [clj-time.core :as t]
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

(deftest extract-data-test
  (testing "Extract function generates filtered-court data for single court"
    (let [data (task/extract-gotcourts 
                (mock-scraper {17 "fixtures/hardhof.edn"})
                [17] (t/now) 50400 64800)]
      (is (= 2 (count 
                (get-in data [17 :courts]))))))
  
  (testing "Extract function generates filtered-court data for 2 courts"
    (let [data (task/extract-gotcourts
                (mock-scraper {17 "fixtures/hardhof.edn"
                               16 "fixtures/lengg.edn"})
                [17 16] (t/now) 50400 60000)]
      (is (= 6 (count (get-in data [17 :courts]))))
      (is (= 1 (count (get-in data [16 :courts])))))))

(deftest handle-alerts-test
  (testing "Test alerts on same data produces empty alert data"
    (let [data       (task/extract-gotcourts 
                      (mock-scraper {17 "fixtures/hardhof.edn"})
                      [17] (t/now) 50400 64800)
          alerts     (task/handle-alerts data data)]
      (is (= nil (get-in alerts [17 :alerts])))))
  
  (testing "Test alerts on new data"
    (let [data       (task/extract-gotcourts 
                      (mock-scraper {17 "fixtures/hardhof.edn"})
                      [17] (t/now) 50400 64800)
          alerts     (task/handle-alerts nil data)]
      (is (= [[:new-slot {:slot {:startTime 50400, :endTime 64800}, :id 86}]
              [:new-slot {:slot {:startTime 50400, :endTime 64800}, :id 88}]] 
             (get-in alerts [17 :alerts]))))))
