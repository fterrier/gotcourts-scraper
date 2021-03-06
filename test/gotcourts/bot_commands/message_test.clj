(ns gotcourts.bot-commands.message-test
  (:require [clj-time
             [core :as t]
             [format :as f]]
            [clojure.test :refer [deftest is testing]]
            [gotcourts.bot-commands.message :as message]))

(def test-date 
  (f/parse (f/formatter "dd-MM-yyyy" (t/default-time-zone)) "27-11-2015"))

(def test-alerts {:alerts 
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
                   :courts [{:id 80 :label "Platz 11"}]}})

(deftest message-test
  (testing "Message on alert for new venue"
    (is (= {:message "The venue TA Mythenquai (Sportamt) has courts available on Fri, 27 Nov 2015 in the time you requested.\n\nThe following slots are available:\n - 09:00-17:00: Platz 11\n"
            :options {:parse-mode :markdown}}
           (message/get-message
            {:success :new-alert 
             :options {:date test-date
                       :new-venue true
                       :alerts-venue-map test-alerts}}))))

  (testing "Message on alert for new venue"
    (is (= {:message "The venue TA Mythenquai (Sportamt) has some changes on Fri, 27 Nov 2015 in the time you requested.\n\nThe following slots are available:\n - 09:00-17:00: Platz 11\nThe following slots are not available any more:\n - 17:00-19:00: Platz 11\n"
            :options {:parse-mode :markdown}}
           (message/get-message
            {:success :new-alert 
             :options {:date test-date
                       :new-venue false
                       :alerts-venue-map test-alerts}}))))
  
  (testing "Message on no alert for venue"
    (is (= {:message "The venue TA Mythenquai (Sportamt) has no courts available on Fri, 27 Nov 2015 in the time you requested.\n\nType /notify to get notified as soon as a court becomes available.",

            :options {:parse-mode :markdown}}
           (message/get-message
            {:success :no-alerts
             :options {:date test-date
                       :chosen-venues {"sportamt" [{:id 5 :name "TA Mythenquai (Sportamt)"}]}}}))))

  (testing "Message on new task"
    (is (= {:message "Got it, will notify you as soon as a court is available at TA Irchel on Fri, 27 Nov 2015 between 11:40 and 13:20."}
           (message/get-message
            {:success :task-added
             :options {:command "/notify"
                       :date test-date
                       :time [42000 48000]
                       :chosen-venues {"irchel" [{:id 5 :name "TA Irchel"}]}}}))))
  
  (testing "Message on ambiguous venue"
    (is (= {:message "Your search for venue matched more than one venue. Please select one below:"
            :options {:parse-mode :markdown
                      :reply-keyboard [[{:text "1. Venue 1"}] [{:text "2. Venue 2"}]]}}
           (message/get-message
            {:success :ambiguous-venue
             :options {:ambiguous-venues ["venue" [{:id 1 :name "Venue 1"}
                                                   {:id 2 :name "Venue 2"}]]}})))))
