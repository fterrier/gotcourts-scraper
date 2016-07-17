(ns gotcourts.checker-test
  (:require [clojure
             [edn :as edn]
             [test :refer :all]]
            [gotcourts.checker :as chk]))

(def core-fixtures
  {:hardhof (edn/read-string (slurp "fixtures/hardhof.edn"))
   :lengg   (edn/read-string (slurp "fixtures/lengg.edn"))})

(deftest test-extract-data
  (testing "Extract simple data"
    (let [data (chk/extract-data (:hardhof core-fixtures))]
      (is (= "Hardhof (Sportamt)" (:name data)))
      (is (= 11 (count (:courts data))))
      (is (= {:id 86
              :label "Platz 6 (WVZ)"
              :surfaceType "clay"
              :type "outdoor"
              :openingTime 28800
              :closingTime 64800
              :free-slots [{:startTime 50400 :endTime 64800}]}
             (get (:courts data) 0))))))

(deftest test-filter-free
  (let [slot1 {:startTime 50400 :endTime 64800}]

    (testing "Free court if exact times"
      (is (= [{:free-slots [slot1]
               :filtered-free-slots [slot1]}]
             (chk/filter-free [{:free-slots [slot1]}] 50400 64800))))
    
    (testing "Free court if time is contained in free"
      (is (= [{:free-slots [slot1]
               :filtered-free-slots [slot1]}]
             (chk/filter-free [{:free-slots [slot1]}] 50400 55000))))
    
    (testing "Free slot with beginning after time is free"
      (is (= [{:free-slots [slot1]
               :filtered-free-slots [slot1]}]
             (chk/filter-free [{:free-slots [slot1]}] 50000 55000))))

    (testing "Free slot with ending before time is free"
      (is (= [{:free-slots [slot1]
               :filtered-free-slots [slot1]}]
             (chk/filter-free [{:free-slots [slot1]}] 60000 65000))))

    (testing "Free slot with ending at beginning is filtered"
      (is (= []
             (chk/filter-free [{:free-slots [slot1]}] 50000 50400))))

    (testing "Free slot with beginning at end is filtered"
      (is (= []
             (chk/filter-free [{:free-slots [slot1]}] 64800 65000))))))
