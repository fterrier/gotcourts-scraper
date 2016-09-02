(ns gotcourts.bot-commands.find-test
  (:require [clojure
             [edn :as edn]
             [test :refer [deftest is testing]]]
            [gotcourts.bot-commands.find :as bot-commands]
            [gotcourts.scraper :as scraper]))

(defn- mock-scraper [{:keys [venue-fixtures availability-fixtures]}]
  (reify scraper/Scrape 
    (fetch-availabilities [_ {:keys [id]}]
      (future (edn/read-string (slurp (get availability-fixtures id)))))
    (fetch-venues [_ {:keys [search]}] 
      (future (edn/read-string (slurp (get venue-fixtures search)))))))

(deftest find-command-test

  (testing "Error outputs error message"
    (let [find-command    (bot-commands/create-find-command nil nil)
          send-to-user-fn (fn [response]
                            (is (= :format-error (:error response)))
                            (is (= :time (:type response))))]
      (find-command nil ["/find" "this" "is" "rubbish"] send-to-user-fn)))

  (testing "Command find responds with free courts"
    (let [send-to-user-fn (fn [response]
                            (is (= :new-alert (:success response)))
                            (is (= [[:new-slot {:slot {:startTime 50400 :endTime 64800} :id 86}]
                                    [:new-slot {:slot {:startTime 50400 :endTime 64800} :id 88}]
                                    [:new-slot {:slot {:startTime 54000 :endTime 64800} :id 90}]] 
                                   (get-in response [:options :alerts-venue-map :alerts])))
                            (is (nil? (:error response))))
          db (atom {})
          scraper (mock-scraper {:venue-fixtures {"asvz" "fixtures/asvz_fluntern.edn"}
                                 :availability-fixtures {6 "fixtures/hardhof.edn"}})
          find-command (bot-commands/create-find-command scraper db)]
      (find-command "user" ["/find" "asvz" "17:00-20:00" "27-11-2015"] send-to-user-fn)
      (is (= 
           {"asvz" [{:id 6, :name "ASVZ Tennisanlage Fluntern"}]}
           (:chosen-venues (first (get-in @db ["user" :find-history])))))))

  (testing "Command find prompts when more than one court"
    (let [send-to-user-fn (fn [response]
                            (is (= :ambiguous-venue (:success response)))
                            (is (= ["asvz" [{:id 6, :name "ASVZ Tennisanlage Fluntern"}
                                            {:id 21, :name "ASVZ Zürich"}]]
                                   (get-in response [:options :ambiguous-venues])))
                            (is (nil? (:error response))))
          db (atom {})
          scraper (mock-scraper {:venue-fixtures {"asvz" "fixtures/asvz.edn"}
                                 :availability-fixtures {6 "fixtures/hardhof.edn"}})
          find-command (bot-commands/create-find-command scraper db)]
      (find-command "user" ["/find" "asvz" "17:00-20:00" "27-11-2015"] send-to-user-fn)
      (is (= 
           {"asvz" [{:id 6, :name "ASVZ Tennisanlage Fluntern"}
                    {:id 21, :name "ASVZ Zürich"}]}
           (:chosen-venues (first (get-in @db ["user" :find-history])))))))

  (testing "Answer to prompt restricts the venues"
    (let [send-to-user-fn (fn [response]
                            (is (= :ambiguous-venue (:success response)))
                            (is (nil? (:error response))))
          db (atom {})
          scraper (mock-scraper {:venue-fixtures {"asvz" "fixtures/asvz.edn"}
                                 :availability-fixtures {6 "fixtures/hardhof.edn"}})
          find-command (bot-commands/create-find-command scraper db)]
      (find-command "user" ["/find" "asvz" "10:00-11:00" "27-11-2015"] send-to-user-fn)
      (find-command "user" ["1." "ASVZ" "Tennisanlage" "Fluntern"] send-to-user-fn)
      (is (= 
           {"asvz" [{:id 6, :name "ASVZ Tennisanlage Fluntern"}]}
           (:chosen-venues (last (get-in @db ["user" :find-history])))))))

  (testing "Command find has no notifications"
    (let [send-to-user-fn (fn [response] 
                            (is (= :no-alerts (:success response)))
                            (is (nil? (:error response))))
          db (atom {})
          scraper (mock-scraper {:venue-fixtures {"asvz" "fixtures/asvz_fluntern.edn"}
                                 :availability-fixtures {6 "fixtures/hardhof.edn"}})
          find-command (bot-commands/create-find-command scraper db)]
      (find-command "user" ["/find" "asvz" "10:00-11:00" "27-11-2015"] send-to-user-fn)
      (is (= 1 (count (get-in @db ["user" :find-history]))))))

  (testing "Command gets added at the end of the history"
    (let [send-to-user-fn (fn [response])
          db (atom {})
          scraper (mock-scraper {:venue-fixtures {"asvz" "fixtures/asvz.edn"
                                                  "tennis" "fixtures/tennis.edn"}
                                 :availability-fixtures 
                                 {6 "fixtures/hardhof.edn"
                                  13860 "fixtures/hardhof.edn"}})
          find-command (bot-commands/create-find-command scraper db)]
      (find-command "user" ["/find" "asvz" "10:00-11:00" "27-11-2015"] send-to-user-fn)
      (find-command "user" ["/find" "tennis" "10:00-11:00" "27-11-2015"] send-to-user-fn)
      (is (= 2 (count (get-in @db ["user" :find-history]))))
      (is (= ["tennis"] (:venues (last (get-in @db ["user" :find-history]))))))))
