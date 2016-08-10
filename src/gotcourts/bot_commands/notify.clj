(ns gotcourts.bot-commands.notify
  (:require [clj-time.core :as t]
            [clojure.tools.logging :as log]
            [gotcourts
             [scraper :as scraper]
             [task :as task]]
            [gotcourts.bot-commands.command :as command]
            [gotcourts.bot-commands.notify-message :as notify-message]))

(defn- start-tasks [schedule-fn tasks]
  (for [{:keys [options task-fn] :as task} tasks]
    (assoc task :stop-fn (schedule-fn options task-fn))))

(defn- get-alert-message [alerts-per-venue is-new date]
  "Takes as a parametere a alerts-per-venue vector:
    - [<venue-id> {:alerts [...] :venue {:name ... :courts ...}}] 
      where each alert in alerts are defined as in the get-alerts task."
  {:success :new-alert :options {:alerts-per-venue alerts-per-venue
                                 :new-venue is-new
                                 :date date}})

(defn- extract-and-alert-task-fn [date notify-fn extract-fn alert-fn]
  (fn [chime-date old-data]
    (log/info "Running notify task for chime" chime-date)
    (when-let [data (extract-fn chime-date)]
      (when-let [alerts-for-all-venues (alert-fn old-data data)]
        (doseq [alerts-per-venue alerts-for-all-venues]
          (notify-fn (get-alert-message alerts-per-venue 
                                        (nil? old-data) 
                                        date))))
      data)))

(defn- add-venue-map [scraper {:keys [venues] :as command}]
  "Given a list of venue terms, retrieves the associated venue ids."
  (let [venue-map     (task/fetch-gotcourts-venues (partial scraper/fetch-venues scraper) venues)
        chosen-venues (->> venue-map
                           (map (fn [[_ venues]] (first venues)))
                           (remove nil?))]
    (assoc command :chosen-venues chosen-venues)))

(defn- add-task-and-respond [scraper notify-fn {:keys [chosen-venues date time] :as command}]
  (let [[start-time end-time] time]
    (letfn [(extract-fn [_] (task/fetch-gotcourts-availabilities 
                             (partial scraper/fetch-availabilities scraper) 
                             (map :id chosen-venues) date start-time end-time))
            (alert-fn [old-data data] (task/get-alerts old-data data))]
      [{:success :task-added :options command}
       [{:task-id (dissoc command :command)
         :command command
         :options {:interval (t/minutes 5) :until (t/plus date (t/seconds end-time))}
         :task-fn (extract-and-alert-task-fn date notify-fn extract-fn alert-fn)}]])))

(defn- create-tasks-and-response [schedule-fn scraper notify-fn command]
  (let [[response new-tasks] (add-task-and-respond scraper 
                                                   notify-fn 
                                                   (add-venue-map scraper command))
        new-started-tasks    (start-tasks schedule-fn new-tasks)]
    [response (merge (->> new-started-tasks
                          (group-by :task-id)
                          (map (fn [[id tasks]] [id (first tasks)]))
                          (into {})))]))

(defn- handle-message* [schedule-fn scraper tasks-db 
                        user command send-to-user-fn]
  (let [tasks                (get @tasks-db user)
        notify-fn            (fn [response] 
                               (send-to-user-fn 
                                (assoc response :text (notify-message/get-message response)))) 
        [response new-tasks] (create-tasks-and-response schedule-fn 
                                                        scraper
                                                        notify-fn 
                                                        command)]
    (swap! tasks-db assoc-in [user :tasks] new-tasks)
    (send-to-user-fn
     (assoc response :text (notify-message/get-message response)))))

(def format-message 
  "Use format /notify <courts> <time> <date>. For example, to get an alert when a court becomes available:
   - /notify 15,14 15:00-17:00 27-11-2016")

(defn create-notify-command [schedule-fn scraper tasks-db]
  (command/create-command
   (partial handle-message* schedule-fn scraper tasks-db)
   [[:venues :list     "Wrong format for venues."]
    [:time   :timespan "Wrong format for time."]
    [:date   :date     "Wrong format for date."]]
   format-message))

