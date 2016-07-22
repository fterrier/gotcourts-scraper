(ns bot.bot-test
  (:require [clojure.test :refer [deftest is testing]]
            [bot.bot :as bot]))

(defn telegram-message [text]
  {:update_id 531750612, 
   :message {:message_id 27, 
             :from {:id 86757011,
                    :first_name "François", 
                    :last_name "Terrier",
                    :username "fterrier"}, 
             :chat {:id 86757011,
                    :first_name "François", 
                    :last_name "Terrier",
                    :username "fterrier", 
                    :type "private"}, 
             :date 1468642768, 
             :text text}})

(deftest parse-message-add-test
  (testing "Error when command not found"
    (let [send-to-user-fn (fn [response command]
                            (is (nil? command))
                            (is (= :command-not-found (:error response))))]
      (bot/handle-message* {} (telegram-message "garbage") send-to-user-fn)))

  (testing "User parses correctly"
    (let [handle-fn (fn [user _ _]
                      (is (= user {:id 86757011
                                   :first_name "François"
                                   :last_name "Terrier"
                                   :username "fterrier"})))]
      (bot/handle-message* {"test" {:handle-fn handle-fn}} (telegram-message "test") nil)))
  
  (testing "Args are passed correctly"
    (let [handle-fn (fn [_ args _]
                      (is (= args ["this" "is" "a" "test"])))]
      (bot/handle-message* {"test" {:handle-fn handle-fn}} (telegram-message " test this is   a  test") nil))))

(deftest create-bot-test
  (testing "Bot with same command twice throws exception"
    (let [commands [{:name "test" :handle-fn nil}
                    {:name "test" :handle-fn nil}]]
      (is (thrown? IllegalArgumentException
                   (bot/create-bot commands)))))
  
  (testing "Bot with good command creates succesfully"
    (let [commands [{:name "test1" :handle-fn (fn [_ _ send-fn] (send-fn {:success :test1}))}
                    {:name "test2" :handle-fn (fn [_ _ _])}]
          bot      (bot/create-bot commands)
          send-to-user-fn (fn [response command]
                            (is (not (nil? command)))
                            (is (= {:success :test1} response)))]
      (is (not (nil? bot)))
      (bot (telegram-message "test1") send-to-user-fn))))
