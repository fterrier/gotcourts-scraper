(ns gotcourts.bot-commands.notify
  (:require [bot.command :as command]
            [clj-time.core :as t]
            [clojure.tools.logging :as log]
            [gotcourts
             [alerter :as alr]
             [scraper :as scraper]
             [task :as task]]
            [gotcourts.bot-commands.message :as message]))

(defn- start-tasks [schedule-fn tasks]
  (for [{:keys [options task-fn] :as task} tasks]
    (assoc task :stop-fn (schedule-fn options task-fn))))

(defn- get-alert-message [alerts-venue-map is-new date]
  "Takes as a parametere a alerts-per-venue vector:
    - [<venue-id> {:alerts [...] :venue {:name ... :courts ...}}] 
      where each alert in alerts are defined as in the get-alerts task."
  {:success :new-alert :options {:alerts-venue-map alerts-venue-map
                                 :new-venue is-new
                                 :date date}})

(defn- extract-and-alert-task-fn [date notify-fn extract-fn alert-fn]
  (fn [chime-date old-data]
    (log/info "Running notify task for chime" chime-date)
    (when-let [data (extract-fn chime-date)]
      (when-let [alerts-for-all-venues (alert-fn old-data data)]
        (doseq [[_ alerts-venue-map] alerts-for-all-venues]
          (notify-fn (get-alert-message alerts-venue-map 
                                        (nil? old-data) 
                                        date))))
      data)))

(defn- add-task-and-respond [scraper notify-fn {:keys [chosen-venues date time] :as command}]
  (let [[start-time end-time] time]
    (letfn [(extract-fn [_] 
              (task/fetch-gotcourts-availabilities 
               (partial scraper/fetch-availabilities scraper) 
               (map :id chosen-venues) date start-time end-time))
            (alert-fn   [old-data data] 
              (alr/get-alerts old-data data))]
      [{:success :task-added :options command}
       [{:task-id (dissoc command :command)
         :command command
         :options {:interval (t/minutes 5)
                   :until (t/plus date (t/seconds end-time))}
         :task-fn (extract-and-alert-task-fn date 
                                             notify-fn extract-fn alert-fn)}]])))

(defn- create-tasks-and-response [schedule-fn scraper notify-fn command]
  (let [[response new-tasks] (add-task-and-respond 
                              scraper notify-fn command)
        new-started-tasks    (start-tasks schedule-fn new-tasks)]
    [response (merge (->> new-started-tasks
                          (group-by :task-id)
                          (map (fn [[id tasks]] [id (first tasks)]))
                          (into {})))]))

(defn- handle-command* [schedule-fn scraper db user command send-to-user-fn]
  (let [tasks                (get @db user)
        notify-fn            (fn [response]
                               (send-to-user-fn (message/assoc-message response))) 
        [response new-tasks] (create-tasks-and-response schedule-fn 
                                                        scraper
                                                        notify-fn 
                                                        command)]
    (swap! db assoc-in [user :tasks] new-tasks)
    (send-to-user-fn (message/assoc-message response))))

(defn- retrieve-from-history [handle-fn db user send-to-user-fn]
  (let [command (last (get-in @db [user :find-history]))]
    (if command
      (handle-fn user command send-to-user-fn)
      (let [response {:error :no-command-history}]
        (send-to-user-fn (message/assoc-message response))))))

(def format-message 
  "Use /find <courts> <time> <date> to look for a court.")

(defn create-notify-command [schedule-fn scraper db]
  (let [handle-fn (partial handle-command* schedule-fn scraper db)]
    (fn [{:keys [user args]} send-to-user-fn]
      (let [[command error] (command/parse-command [] (rest args) format-message)]
        (if error
          (send-to-user-fn error)
          (retrieve-from-history handle-fn db user send-to-user-fn))))))

