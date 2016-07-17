(ns scheduler.scheduler-test
  (:require [scheduler.scheduler :as scheduler]
            [clj-time.core :as t]
            [clojure.core.async :refer [<!! timeout]]
            [clojure.test :refer [deftest is testing]]))

(deftest test-scheduler
  (testing "Adds a chime and stopping it works"
    (let [scheduler (scheduler/init-scheduler)
          interval  (t/millis 100)
          test-atom (atom 0)
          task-fn   (fn [_ _] (swap! test-atom inc))
          stop-fn   (scheduler/add-chime scheduler interval task-fn)]
      (<!! (timeout 150))
      (is (= @test-atom 1))
      (stop-fn)
      (<!! (timeout 150))
      (is (= @test-atom 1))))
  
  (testing "Adding a chime with faulty function does not crash the task"
    (let [scheduler (scheduler/init-scheduler)
          interval  (t/millis 100)
          test-atom (atom 0)
          task-fn   (fn [_ _]
                      (swap! test-atom inc)
                      (throw (Exception.)))
          stop-fn   (scheduler/add-chime scheduler interval task-fn)]
      (<!! (timeout 160))
      (is (= @test-atom 1))
      (<!! (timeout 160))
      (is (> @test-atom 1))
      (stop-fn)
      (<!! (timeout 150))
      (is (= @test-atom 3))))

  (testing "Stop all chimes stops all"
    (let [scheduler (scheduler/init-scheduler)
          interval  (t/millis 100)
          test-atom (atom 0)
          task-fn   (fn [_ _] (swap! test-atom inc))
          stop-fn   (scheduler/add-chime scheduler interval task-fn)]
      (<!! (timeout 150))
      (is (= @test-atom 1))
      (scheduler/stop-chimes scheduler)
      (<!! (timeout 150))
      (is (= @test-atom 1))
      (stop-fn))))
