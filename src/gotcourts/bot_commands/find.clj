(ns gotcourts.bot-commands.find
  (:require [bot.command :as command]
            [gotcourts
             [alerter :as alr]
             [scraper :as scraper]
             [task :as task]]
            [gotcourts.bot-commands.message :as message]
            [clojure.tools.logging :as log]))

(defn- add-venue-map [scraper {:keys [venues] :as command}]
  "Given a list of venue terms, retrieves the associated venue ids."
  (let [venue-map (task/fetch-gotcourts-venues 
                   (partial scraper/fetch-venues scraper) venues)]
    (assoc command :chosen-venues venue-map)))

(defn- ambiguous-venues [{:keys [chosen-venues] :as command}]
  "Returns a map of venue lists - {<term> [<venue>, ...]}"
  (->> chosen-venues
       (remove (fn [[_ venues]] (= 1 (count venues))))
       (into {})))

(defn- send-alerts
  [scraper user {:keys [chosen-venues time date] :as command} send-to-user-fn]
  (let [[start-time end-time] time
          availabilities (task/fetch-gotcourts-availabilities 
                          (partial scraper/fetch-availabilities scraper) 
                          (map (fn [[_ venues]] (:id (first venues))) chosen-venues) 
                          date start-time end-time)
          alerts         (alr/get-alerts nil availabilities)]
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

(defn- send-prompt-venue [ambiguous-venues user {:keys [date] :as command} 
                          send-to-user-fn]
  (send-to-user-fn (message/assoc-message 
                    {:success :ambiguous-venue
                     :options {:date date
                               :ambiguous-venues (first ambiguous-venues)}})))

(defn- handle-message* [scraper db user {:keys [chosen-venues] :as command} 
                        send-to-user-fn]
  (log/info "Handling command" command)
  (let [{:keys [chosen-venues date time] :as command} 
        (if-not chosen-venues (add-venue-map scraper command) command)
        ambiguous-venues (ambiguous-venues command)]
    (swap! db update-in [user :find-history] concat [command])
    (if-not (empty? ambiguous-venues)
      (send-prompt-venue ambiguous-venues user command send-to-user-fn)
      (send-alerts scraper user command send-to-user-fn))))

(def format-message
  "Use format /find <courts> <time> <date>. For example, to get an alert when a court becomes available:
   - /find asvz 15:00-17:00 27-11-2016")

(defn- parse-index [text]
  (try
    (dec (Integer/parseInt (re-find #"^\d*" text)))
    (catch Exception e
      (log/warn e "Could not parse index " text))))

(defn- restrict-chosen-venues [command index]
  (let [[term venues]         (first (ambiguous-venues command))
        chosen-venue-for-term (get-in command [:chosen-venues term index])]
    (if (nil? chosen-venue-for-term) command
        (assoc-in command [:chosen-venues term] [chosen-venue-for-term]))))

(defn- get-command [db user [first-arg & rest-args] type-format-message-args]
  "Either returns a new command if /find was received or retrieves a command
   from the history and restricts the venue if a number (ex: 2.) was received."
  (if (= "/find" first-arg)
    (command/parse-command type-format-message-args rest-args format-message)
    (let [last-command       (last (get-in @db [user :find-history]))
          chosen-venue-index (parse-index first-arg)]
      (log/info "Retrieved last command from history" last-command)
      (if-not last-command
        [nil :no-command-history]
        [(restrict-chosen-venues last-command chosen-venue-index) nil]))))

(defn create-find-command [scraper db]
  (let [type-format-message-args [[:venues :list     "Wrong format for venues."]
                                  [:time   :timespan "Wrong format for time."]
                                  [:date   :date     "Wrong format for date."]]
        handle-fn                (partial handle-message* scraper db)]
    (fn [{:keys [user args]} send-to-user-fn]
      (let [[command error] (get-command db user args type-format-message-args)]
        (if error 
          (send-to-user-fn error)
          (handle-fn user command send-to-user-fn))))))
