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

;; (deftest bot-test
;;   (testing "Error outputs error message and no db change"
;;     (let [find-command    (bot-commands/create-find-command nil nil)
;;           send-to-user-fn (fn [response]
;;                             (is (= :format-error (:error response)))
;;                             (is (= :time (:type response))))]
;;       (find-command nil ["this" "is" "rubbish"] send-to-user-fn)))

;;   (testing "Command find responds with court data - fetches venue and availability properly"
;;     (let [send-to-user-fn (fn [response] 
;;                             (is (= :task-added (:success response)))
;;                             (is (nil? (:error response))))
;;           tasks-db (atom {})
;;           scraper (mock-scraper {:venue-fixtures {"asvz" "fixtures/asvz.edn"}})
;;           find-command (bot-commands/create-find-command scraper tasks-db)]
;;       (find-command "user" ["asvz" "10:00-11:00" "27-11-2015"] send-to-user-fn)
;;       (is (= 1 (count @tasks-db)))
;;       (let [tasks    (get @tasks-db "user")
;;             [_ task] (first tasks)]
;;         (is (= :mock-stop-fn (:stop-fn task)))))))
