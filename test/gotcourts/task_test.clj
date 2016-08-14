(ns gotcourts.task-test
  (:require [clj-time
             [core :as t]
             [format :as f]]
            [clojure
             [edn :as edn]
             [test :refer [deftest is testing]]]
            [gotcourts.task :as task]))

(defn mock-availability-fn [fixture-map]
  (fn [{:keys [id]}]
    (future (edn/read-string (slurp (get fixture-map id))))))

(defn mock-venue-fn [fixture-map]
  (fn [{:keys [search]}]
    (future (edn/read-string (slurp (get fixture-map search))))))

(defn mock-notifier [alert-atom]
  (fn [alert-data]
    (reset! alert-atom alert-data)))

(deftest fetch-availabilities-test
  (testing "Extract function uses right date"
    (let [test-date 
          (f/parse (f/formatter "dd-MM-yyyy" (t/default-time-zone)) "27-11-2015")]
      (task/fetch-gotcourts-availabilities
       (fn [{:keys [date]}] 
         (is (= "2015-11-27" date))
         (future {}))
       [1] test-date 50000 60000)))

  (testing "Extract function generates filtered-court data for single court"
    (let [data (task/fetch-gotcourts-availabilities 
                (mock-availability-fn {17 "fixtures/hardhof.edn"})
                [17] (t/now) 50400 64800)]
      (is (= 8 (count 
                (get-in data [17 :courts]))))))
  
  (testing "Extract function generates filtered-court data for 2 courts"
    (let [data (task/fetch-gotcourts-availabilities
                (mock-availability-fn {17 "fixtures/hardhof.edn"
                               16 "fixtures/lengg.edn"})
                [17 16] (t/now) 50400 60000)]
      (is (= 9 (count (get-in data [17 :courts]))))
      (is (= 4 (count (get-in data [16 :courts])))))))

(deftest fetch-venues-test
  (testing "Test get venues several at once"
    (let [data (task/fetch-gotcourts-venues
                (mock-venue-fn {"tennis" "fixtures/tennis.edn"
                                "asvz"   "fixtures/asvz.edn"})
                ["tennis" "asvz"])]
      (is (= {"tennis"
              [{:id 13860 :name "0 TENNIS CLUB"}
               {:id 23584 :name "1. Halleiner Tennisclub"}
               {:id 32854 :name "1. Kieler Hockey- und Tennisclub von 1907 e. V."}
               {:id 21556 :name "1. Klosterneuburger Tennisverein"}
               {:id 33139 :name "1. Tennis Sport Club Breitenfelde"}],
              "asvz"
              [{:id 6  :name "ASVZ Tennisanlage Fluntern"}
               {:id 21 :name "ASVZ Zürich"}]} 
             data)))))
