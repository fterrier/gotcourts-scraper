(ns gotcourts.bot-commands.show-test
  (:require [clojure.test :refer [deftest is testing]]
            [gotcourts.bot-commands.show :as bot-commands]))

(deftest show-command-test
  (testing "Rubbish after /show is ok"
    (let [tasks-db        (atom {})
          show-command    (bot-commands/create-show-command tasks-db)
          send-to-user-fn (fn [response] (is (= {:success :show :options {}}) response))]
      (show-command nil ["this" "is" "rubbish"] send-to-user-fn)))

  (testing "Commands :show shows the tasks"
    (let [tasks-db        (atom {:test-task {:task-id :test-task}})
          show-command    (bot-commands/create-show-command tasks-db)
          send-to-user-fn (fn [response] (is (= {:success :show :options {:test-task {:task-id :test-task}}}) 
                                             response))]
      (show-command nil ["this" "is" "rubbish"] send-to-user-fn))))
