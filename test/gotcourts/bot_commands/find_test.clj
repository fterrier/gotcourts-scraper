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
      (find-command nil ["this" "is" "rubbish"] send-to-user-fn)))

  (testing "Command find responds with alerts"
    (let [send-to-user-fn (fn [response] 
                            (is (= :new-alert (:success response)))
                            (is (nil? (:error response))))
          db (atom {})
          scraper (mock-scraper {:venue-fixtures {"asvz" "fixtures/asvz.edn"}
                                 :availability-fixtures {6 "fixtures/hardhof.edn"}})
          find-command (bot-commands/create-find-command scraper db)]
      (find-command "user" ["asvz" "17:00-20:00" "27-11-2015"] send-to-user-fn)
      (is (= 1 (count (get-in @db ["user" :find-history]))))))

  (testing "Command find has no alerts"
    (let [send-to-user-fn (fn [response] 
                            (is (= :no-alerts (:success response)))
                            (is (nil? (:error response))))
          db (atom {})
          scraper (mock-scraper {:venue-fixtures {"asvz" "fixtures/asvz.edn"}
                                 :availability-fixtures {6 "fixtures/hardhof.edn"}})
          find-command (bot-commands/create-find-command scraper db)]
      (find-command "user" ["asvz" "10:00-11:00" "27-11-2015"] send-to-user-fn)
      (is (= 1 (count (get-in @db ["user" :find-history]))))))

    (testing "Command gets added at the end of the history"
    (let [send-to-user-fn (fn [response])
          db (atom {"user" {:find-history [:history1]}})
          scraper (mock-scraper {:venue-fixtures {"asvz" "fixtures/asvz.edn"}
                                 :availability-fixtures {6 "fixtures/hardhof.edn"}})
          find-command (bot-commands/create-find-command scraper db)]
      (find-command "user" ["asvz" "10:00-11:00" "27-11-2015"] send-to-user-fn)
      (is (= 2 (count (get-in @db ["user" :find-history]))))
      (is (not= :history1 (last (get-in @db ["user" :find-history])))))))
