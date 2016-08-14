(ns gotcourts.bot-commands.find
  (:require [bot.command :as command]
            [gotcourts
             [alerter :as alr]
             [scraper :as scraper]
             [task :as task]]
            [gotcourts.bot-commands.message :as message]))

(defn add-venue-map [scraper {:keys [venues] :as command}]
  "Given a list of venue terms, retrieves the associated venue ids."
  (let [venue-map     (task/fetch-gotcourts-venues (partial scraper/fetch-venues scraper) venues)
        chosen-venues (->> venue-map
                           (map (fn [[_ venues]] (first venues)))
                           (remove nil?))]
    (assoc command :chosen-venues chosen-venues)))

(defn- handle-message* [scraper db user command send-to-user-fn]
  (let [{:keys [chosen-venues date time] :as command} 
        (add-venue-map scraper command)
        [start-time end-time] time
        availabilities (task/fetch-gotcourts-availabilities 
                        (partial scraper/fetch-availabilities scraper) 
                        (map :id chosen-venues) date start-time end-time)
        alerts         (alr/get-alerts nil availabilities)]
    (swap! db update-in [user :find-history] concat [command])
    (if-not (empty? alerts)
      (doseq [[_ alerts-venue-map] alerts]
        (let [response {:success :new-alert 
                        :options {:alerts-venue-map alerts-venue-map
                                  :new-venue true
                                  :date date}}]
          (send-to-user-fn (message/assoc-message response))))
      (do
        (send-to-user-fn (message/assoc-message {:success :no-alerts
                                                 :options command}))))))

(def format-message 
  "Use format /find <courts> <time> <date>. For example, to get an alert when a court becomes available:
   - /find asvz 15:00-17:00 27-11-2016")

(defn create-find-command [scraper db]
  (let [type-format-message-args [[:venues :list     "Wrong format for venues."]
                                  [:time   :timespan "Wrong format for time."]
                                  [:date   :date     "Wrong format for date."]]
        handle-fn                (partial handle-message* scraper db)]
    (fn [user args send-to-user-fn]
      (let [[command error] (command/parse-command type-format-message-args 
                                                   args format-message)]
        (if error
          (send-to-user-fn error)
          (handle-fn user command send-to-user-fn))))))
