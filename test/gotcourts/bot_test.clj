(ns gotcourts.bot-test
  (:require [clojure.test :refer [deftest is testing]]
            [gotcourts.bot :as bot]))

(deftest bot-test
  (testing "Error outputs error message and no db change"
    (let [[response tasks] 
          (bot/create-tasks-and-response nil nil nil 
                              {} {:error {:error :test :text "error"}})]
      (is (= :test (:error response)))
      (is (= {} tasks))))
  
  (testing "Commands :add adds the task and responds"
    (let [command {:command :add :courts [1 2 3]}
          schedule-fn (fn [_ _] :mock-stop-fn)
          create-task-fn (fn [_])
          notify-fn (fn [_])
          [response tasks] (bot/create-tasks-and-response schedule-fn create-task-fn notify-fn
                                               {} {:command command})]
      (is (= :task-added (:success response)))
      (let [task (get tasks {:courts [1 2 3]})]
        (is (= :mock-stop-fn (:stop-fn task)))
        (is (= {:courts [1 2 3]} (:task-id task))))))
  
  (testing "Commands :stop stops all tasks"
    (let [command {:command :delete-all}
          schedule-fn (fn [_ _] :mock-stop-fn)
          create-task-fn (fn [_])
          notify-fn (fn [_])
          [response tasks] (bot/create-tasks-and-response schedule-fn create-task-fn notify-fn
                                               {:test-task {:task-id :test-task :stop-fn (fn [])}} 
                                               {:command command})]
      (is (= :task-deleted-all (:success response)))
      (is (= {} tasks))))
  
  (testing "Commands :show shows the tasks"
    (let [command {:command :show}
          schedule-fn (fn [_ _])
          create-task-fn (fn [_])
          notify-fn (fn [_])
          user-tasks {:test-task {:task-id :test-task}}
          [response tasks] (bot/create-tasks-and-response schedule-fn create-task-fn notify-fn
                                               user-tasks
                                               {:command command})]
      (is (= user-tasks tasks))
      (is (= {:success :show :options user-tasks} response)))))
