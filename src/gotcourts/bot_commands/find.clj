(ns gotcourts.bot-commands.find
  (:require [bot.command :as command]
            [gotcourts
             [alerter :as alr]
             [scraper :as scraper]
             [task :as task]]
            [gotcourts.bot-commands
             [common :as common]
             [message :as message]]))

(defn- handle-message* [scraper db user command send-to-user-fn]
  (let [{:keys [chosen-venues date time] :as command} 
                       (common/add-venue-map scraper command)
        [start-time end-time] time
        availabilities (task/fetch-gotcourts-availabilities 
                        (partial scraper/fetch-availabilities scraper) 
                        (map :id chosen-venues) date start-time end-time)
        alerts         (alr/get-alerts nil availabilities)]
    (swap! db update-in [user :find-history] conj command)
    (if-not (empty? alerts)
      (doseq [[_ alerts-venue-map] alerts]
        (let [response {:success :new-alert 
                        :options {:alerts-venue-map alerts-venue-map
                                  :new-venue true
                                  :date date}}]
          (send-to-user-fn (assoc response
                                  :text (message/get-message response)))))
      (do
        (send-to-user-fn {:success :no-alerts
                          :options {:date date}})))))

(def format-message 
  "Use format /find <courts> <time> <date>. For example, to get an alert when a court becomes available:
   - /find asvz 15:00-17:00 27-11-2016")

(defn create-find-command [scraper db]
  (command/create-command
   (partial handle-message* scraper db)
   [[:venues :list     "Wrong format for venues."]
    [:time   :timespan "Wrong format for time."]
    [:date   :date     "Wrong format for date."]]
   format-message))
