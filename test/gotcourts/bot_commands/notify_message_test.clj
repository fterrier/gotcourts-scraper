(ns gotcourts.bot-commands.notify-message-test
  (:require [clj-time.format :as f]
            [clojure.test :refer [deftest is testing]]
            [gotcourts.bot-commands.notify-message :as bot-notify-message]
            [clj-time.core :as t]))

(def test-date 
  (f/parse (f/formatter "dd-MM-yyyy" (t/default-time-zone)) "27-11-2015"))

(def test-alerts [15 {:alerts 
                      [[:new-slot {:slot {:startTime 32400 
                                          :endTime 61200} 
                                   :id 80}]
                       [:gone-slot {:slot {:startTime 61200 
                                           :endTime 68400} 
                                    :id 80}]]
                      :venue
                      {:name "TA Mythenquai (Sportamt)" 
                       :city "Zürich"
                       :country "CH"
                       :web "tinyurl.com/nqecu9w"
                       :phone " 044 202 32 43"
                       :courts [{:id 80 :label "Platz 11"}]}}])

(deftest message-test
  (testing "Message on alert for new venue"
    (is (= {:message "The venue TA Mythenquai (Sportamt) has courts available on Fri, 27 Nov 2015 in the time you requested.\n\nThe following slots are available:\n - 09:00-17:00: Platz 11\n"
            :options {:parse-mode :markdown}}
           (bot-notify-message/get-message
            {:success :new-alert 
             :options {:date test-date
                       :new-venue true
                       :alerts-per-venue test-alerts}}))))

  (testing "Message on alert for new venue"
    (is (= {:message "The venue TA Mythenquai (Sportamt) has some changes on Fri, 27 Nov 2015 in the time you requested.\n\nThe following slots are available:\n - 09:00-17:00: Platz 11\nThe following slots are not available any more:\n - 17:00-19:00: Platz 11\n"
            :options {:parse-mode :markdown}}
           (bot-notify-message/get-message
            {:success :new-alert 
             :options {:date test-date
                       :new-venue false
                       :alerts-per-venue test-alerts}})))))
