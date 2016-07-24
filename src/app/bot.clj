(ns app.bot
  (:require [app.scheduler :as scheduler]
            [bot.bot :as bot]
            [clojure.core.async :refer [>!!]]
            [gotcourts
             [scraper :as scraper]
             [task :as task]]
            [gotcourts.bot-commands
             [notify :as notify-command]
             [show :as show-command]]
            [mount.core :refer [defstate]]
            [ring.util.response :refer [response]]
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

(defn- get-message [{:keys [error success] :as response}]
  (cond
    (= :command-not-found error) 
    {:message "Sorry, did not understand this. Use /help to get help."}
    :else {:message (str response)}))

(defn- send-to-user [ch {:keys [text] :as response} command]
  (>!! ch 
       (if (nil? text)
         (get-message response)
         text)))

(defn- start-bot []
  (let [tasks-db   (atom {})
        commands   [{:name "/notify"   :handle-fn (create-notify-command tasks-db)}
                    {:name "/show" :handle-fn (create-show-command tasks-db)}]
        bot        (bot/create-bot commands)]
    (fn [data ch]
      (bot data (partial send-to-user ch)))))

(defstate bot :start (start-bot))
