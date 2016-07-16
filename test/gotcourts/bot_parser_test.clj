(ns gotcourts.bot-parser-test
  (:require [clojure.test :refer [deftest is testing]]
            [gotcourts.bot-parser :as bot-parser]
            [clj-time.format :as f]))

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

(deftest parse-message-test
  (testing "Message user parses correctly"
    (let [{:keys [user]} (bot-parser/parse-message (telegram-message ""))]
      (is (= user {:id 86757011
                   :first_name "François"
                   :last_name "Terrier"
                   :username "fterrier"}))))
  
  (testing "Message with garbage produces error"
    (let [{:keys [error]} (bot-parser/parse-message (telegram-message "garbage"))]
      (is (= :not-found (:error error)))))
  
  (testing "Correct message is parsed properly"
    (let [{:keys [command error]} (bot-parser/parse-message 
                                   (telegram-message "/notify 15,16,17 17:00-21:00 27-11-2015"))]
      (is (nil? error))
      (is (= {:command :notify
              :courts [15 16 17]
              :start-time 61200
              :end-time 75600
              :date (f/parse (f/formatter "dd-MM-yyyy") "27-11-2015")} command))))

  ;(testing "Bad time")
  ;(testing "Bad courts)
  ;(testing "Bad date)
  ;(testing "Too many arguments)
  )

