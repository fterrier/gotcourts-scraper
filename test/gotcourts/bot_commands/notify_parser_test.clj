(ns gotcourts.bot-commands.notify-parser-test
  (:require [clj-time 
             [format :as f]
             [predicates :as pr]]
            [clojure.test :refer [deftest is testing]]
            [gotcourts.bot-commands.notify-parser :as bot-commands]
            [clj-time.core :as t]))

(def test-date (f/parse (f/formatter "dd-MM-yyyy" (t/default-time-zone)) "27-11-2015"))

(defn- test-result 
  ([start-time end-time]
   (test-result start-time end-time test-date))
  ([start-time end-time date]
   {:command :add
    :venues ["15" "16" "17"]
    :start-time start-time
    :end-time end-time
    :date date}))

(deftest parse-message-add-test
  (testing "Correct message is parsed properly"
    (let [[command error] (bot-commands/parse-command-chunks ["15,16,17" "17:00-21:00" "27-11-2015"])]
      (is (= (test-result 61200 75600) 
             command))
      (is (nil? error))))
  
  (testing "Correct message is parsed properly - hours in HH'h'"
    (let [[command error] (bot-commands/parse-command-chunks ["15,16,17" "17h-21h" "27-11-2015"])]
      (is (= (test-result 61200 75600)
             command))
      (is (nil? error))))

  (testing "Correct message is parsed properly - hours in HH'h'mm"
    (let [[command error] (bot-commands/parse-command-chunks ["15,16,17" "17h00-21h30" "27-11-2015"])]
      (is (= (test-result 61200 77400)
             command))
      (is (nil? error))))

  (testing "Correct message is parsed properly - date in dd/MM/yyyy"
    (let [[command error] (bot-commands/parse-command-chunks ["15,16,17" "17h00-21h30" "27-11-2015"])]
      (is (= (test-result 61200 77400)
             command))
      (is (nil? error))))

  (testing "Correct message is parsed properly - date in dd/MM"
    (let [[command error] (bot-commands/parse-command-chunks ["15,16,17" "17h00-21h30" "27/11/2015"])]
      (is (= (test-result 61200 77400)
             command))
      (is (nil? error))))

  ;; (testing "Correct message is parsed properly - date in dd/MM"
  ;;   (let [[command error] (bot-commands/parse-command-chunks ["15,16,17" "17h00-21h30" "27/11"])]
  ;;     (is (= (test-result 61200 77400)
  ;;            command))
  ;;     (is (nil? error))))

  (testing "Correct message is parsed properly - date <monday>"
    (let [[command error] (bot-commands/parse-command-chunks ["15,16,17" "17h00-21h30" "monday"])]
      (is (pr/monday? (:date command)))
      (is (t/after? (:date command) (t/now)))
      (is (nil? error))))

  (testing "Correct message is parsed properly - date <on monday>"
    (let [[command error] (bot-commands/parse-command-chunks ["15,16,17" "17h00-21h30" "monday"])]
      (is (pr/monday? (:date command)))
      (is (t/after? (:date command) (t/now)))
      (is (nil? error))))

  ;(testing "Bad time")
  ;(testing "Bad courts)
  ;(testing "Bad date)
  ;(testing "Too many arguments)
  )

