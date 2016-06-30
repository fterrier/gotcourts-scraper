(ns gotcourts.task-test
  (:require [clj-time.core :as t]
            [clojure
             [edn :as edn]
             [test :refer [deftest is testing]]]
            [gotcourts.task :as task]))

(defn mock-scraper [fixture]
  (fn [_]
    (edn/read-string (slurp fixture))))

(deftest extract-data-test
  (testing "Extract function generates filtered-court data"
        (is (= 2
               (count (task/extract-gotcourts (mock-scraper "fixtures/hardhof.edn") 17 (t/now) 50400 64800))))))
