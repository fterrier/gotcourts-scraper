(ns gotcourts.bot-commands.notify-test
  (:require [clojure
             [edn :as edn]
             [test :refer [deftest is testing]]]
            [gotcourts.bot-commands.notify :as bot-commands]
            [gotcourts.scraper :as scraper]))

(defn- mock-scraper [{:keys [venue-fixtures]}]
  (reify scraper/Scrape 
    (fetch-availabilities [_ params] )
    (fetch-venues [_ {:keys [search]}] 
      (println venue-fixtures)
      (future (edn/read-string (slurp (get venue-fixtures search)))))))

(deftest bot-test
  (testing "Error outputs error message and no db change"
    (let [notify-command  (bot-commands/create-notify-command nil nil nil)
          send-to-user-fn (fn [response] 
                            (is (= :format-error (:error response)))
                            (is (= :time (:type response))))]
      (notify-command nil ["this" "is" "rubbish"] send-to-user-fn)))

  (testing "Command notify adds the task and respond success"
    (let [schedule-fn (fn [_ _] :mock-stop-fn)
          send-to-user-fn (fn [response] 
                            (is (= :task-added (:success response)))
                            (is (nil? (:error response))))
          tasks-db (atom {})
          scraper (mock-scraper {:venue-fixtures {"1" "fixtures/asvz.edn"}})
          notify-command (bot-commands/create-notify-command schedule-fn scraper tasks-db)]
      (notify-command "user" ["1" "10:00-11:00" "27-11-2015"] send-to-user-fn)
      (is (= 1 (count @tasks-db)))
      (let [tasks    (get @tasks-db "user")
            [_ task] (first tasks)]
        (is (= :mock-stop-fn (:stop-fn task))))))
  
  (testing "Venues are properly fetched"
    (let [schedule-fn (fn [_ _])
          send-to-user-fn (fn [response])
          tasks-db (atom {})
          scraper (mock-scraper {:venue-fixtures {"1" "fixtures/asvz.edn"}})
          notify-command (bot-commands/create-notify-command schedule-fn scraper tasks-db)]
      (notify-command "user" ["1" "10:00-11:00" "27-11-2015"] send-to-user-fn)
      (is (= 1 (count @tasks-db)))
      (let [tasks    (get @tasks-db "user")
            [_ task] (first tasks)]
        (is (= [{:id 6 :name "ASVZ Tennisanlage Fluntern"}] (:chosen-venues (:command task))))))))

  
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
  

