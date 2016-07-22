(ns app.bot
  (:require [app.scheduler :as scheduler]
            [bot.bot :as bot]
            [clojure.core.async :refer [>!!]]
            [gotcourts
             [scraper :as scraper]
             [task :as task]]
            [gotcourts.bot-commands
             [bot-messages :as bot-messages]
             [notify :as notify-command]
             [show :as show-command]]
            [mount.core :refer [defstate]]
            [scheduler.scheduler :as task-scheduler]))

(defn- create-scraper []
  (let [scraper (scraper/gotcourts-scraper)]
    (fn [params] 
      (scraper/retrieve-raw-data scraper params))))

(defn- create-show-command [tasks-db]
  (show-command/create-show-command tasks-db))

(defn- create-notify-command [tasks-db]
  (let [schedule-fn    (fn [interval task-fn] 
                         (task-scheduler/add-chime scheduler/scheduler 
                                                   interval task-fn))
        create-task-fn (task/create-gotcourts-task-creator (create-scraper))]
    (notify-command/create-notify-command schedule-fn create-task-fn tasks-db)))

(defn- send-to-user [ch response command]
  (>!! ch (bot-messages/unparse response command)))

(defn- start-bot []
  (let [tasks-db   (atom {})
        commands   [{:name "/notify"   :handle-fn (create-notify-command tasks-db)}
                    {:name "/show" :handle-fn (create-show-command tasks-db)}]
        bot        (bot/create-bot commands)]
    (fn [data ch]
      (bot data (partial send-to-user ch)))))

(defstate bot :start (start-bot))
