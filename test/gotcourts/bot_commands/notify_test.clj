(ns gotcourts.bot-commands.notify-test
  (:require [clj-time.format :as f]
            [clojure.test :refer [deftest is testing]]
            [gotcourts.bot-commands.notify :as bot-commands]))

(deftest parse-message-add-test
   (testing "Correct message is parsed properly"
     (let [[command error] (bot-commands/parse-command-chunks ["15,16,17" "17:00-21:00" "27-11-2015"])]
       (is (= {:command :add
               :courts [15 16 17]
               :start-time 61200
               :end-time 75600
               :date (f/parse (f/formatter "dd-MM-yyyy") "27-11-2015")}
              command))
       (is (nil? error))))

  ;(testing "Bad time")
  ;(testing "Bad courts)
  ;(testing "Bad date)
  ;(testing "Too many arguments)
)

(deftest bot-test
  (testing "Error outputs error message and no db change"
    (let [notify-command  (bot-commands/create-notify-command nil nil nil)
          send-to-user-fn (fn [response] (is (= {:error :format-error :type :courts} response)))]
      (notify-command nil ["this" "is" "rubbish"] send-to-user-fn)))

  (testing "Command notify adds the task and respond success"
    (let [schedule-fn (fn [_ _] :mock-stop-fn)
          create-task-fn (fn [_])
          send-to-user-fn (fn [response] (is (= {:success :task-added} response)))
          tasks-db (atom {})
          notify-command (bot-commands/create-notify-command schedule-fn create-task-fn tasks-db)]
      (notify-command "user" ["1" "10:00-11:00" "27-11-2015"] send-to-user-fn)
      (is (= 1 (count @tasks-db)))
      (let [tasks    (get @tasks-db "user")
            [_ task] (first tasks)]
        (is (= :mock-stop-fn (:stop-fn task))))))
  
  
  
  ;; (testing "Commands :stop stops all tasks"
  ;;   (let [command {:command :delete-all}
  ;;         schedule-fn (fn [_ _] :mock-stop-fn)
  ;;         create-task-fn (fn [_])
  ;;         notify-fn (fn [_])
  ;;         [response tasks] (bot/create-tasks-and-response schedule-fn create-task-fn notify-fn
  ;;                                              {:test-task {:task-id :test-task :stop-fn (fn [])}} 
  ;;                                              {:command command})]
  ;;     (is (= :task-deleted-all (:success response)))
  ;;     (is (= {} tasks))))
  )

