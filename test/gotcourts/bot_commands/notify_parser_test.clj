(ns gotcourts.bot-commands.notify-parser-test
  (:require [clj-time.format :as f]
            [clojure.test :refer [deftest is testing]]
            [gotcourts.bot-commands.notify-parser :as bot-commands]))

(deftest parse-message-add-test
   (testing "Correct message is parsed properly"
     (let [[command error] (bot-commands/parse-command-chunks ["15,16,17" "17:00-21:00" "27-11-2015"])]
       (is (= {:command :add
               :venues [15 16 17]
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
