(ns scheduler.scheduler-test
  (:require [scheduler.scheduler :as scheduler]
            [clj-time.core :as t]
            [clojure.core.async :refer [<!! timeout]]
            [clojure.test :refer [deftest is testing]]))

(defn- scheduler-empty? [scheduler]
  (empty? @scheduler))

(deftest test-scheduler
  (testing "Adds a chime and stopping it works"
    (let [scheduler (scheduler/init-scheduler)
          interval  (t/millis 100)
          test-atom (atom 0)
          task-fn   (fn [_ _] (swap! test-atom inc))
          stop-fn   (scheduler/add-chime scheduler {:interval interval} task-fn)]
      (<!! (timeout 250))
      (is (= @test-atom 2))
      (stop-fn)
      (<!! (timeout 150))
      (is (= @test-atom 2))
      (is (scheduler-empty? scheduler))))
  
  (testing "Adding a chime with faulty function does not crash the task"
    (let [scheduler (scheduler/init-scheduler)
          interval  (t/millis 100)
          test-atom (atom 0)
          task-fn   (fn [_ _]
                      (swap! test-atom inc)
                      (throw (Exception.)))
          stop-fn   (scheduler/add-chime scheduler {:interval interval} task-fn)]
      (<!! (timeout 260))
      (is (= @test-atom 2))
      (<!! (timeout 160))
      (is (> @test-atom 2))
      (stop-fn)
      (<!! (timeout 150))
      (is (= @test-atom 4))
      (is (scheduler-empty? scheduler))))

  (testing "Stop all chimes stops all"
    (let [scheduler (scheduler/init-scheduler)
          interval  (t/millis 100)
          test-atom (atom 0)
          task-fn   (fn [_ _] (swap! test-atom inc))
          stop-fn   (scheduler/add-chime scheduler {:interval interval} task-fn)]
      (<!! (timeout 250))
      (is (= @test-atom 2))
      (scheduler/stop-chimes scheduler)
      (<!! (timeout 150))
      (is (= @test-atom 2))
      (stop-fn)
      (is (scheduler-empty? scheduler))))
  
  (testing "Defining until stops chimes after until is passed"
    (let [scheduler (scheduler/init-scheduler)
          interval  (t/millis 100)
          until     (t/plus (t/now) (t/millis 250))
          test-atom (atom 0)
          task-fn   (fn [_ _] (swap! test-atom inc))
          stop-fn   (scheduler/add-chime scheduler {:interval interval :until until} task-fn)]
      (<!! (timeout 350))
      (is (= @test-atom 2))
      (<!! (timeout 150))
      (is (= @test-atom 2))
      (is (scheduler-empty? scheduler))
      (stop-fn)
      (is (scheduler-empty? scheduler)))))
