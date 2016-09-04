(ns app.bot
  (:require [app
             [database :as database]
             [scheduler :as scheduler]
             [scraper :as scraper]]
            [bot.bot :as bot]
            [bot.matcher :as matcher]
            [clojure.core.async :refer [>!!]]
            [gotcourts.bot-commands
             [notify :as notify-command]
             [show :as show-command]
             [find :as find-command]]
            [mount.core :refer [defstate]]
            [ring.util.response :refer [response]]
            [scheduler.scheduler :as task-scheduler]))

(defn- create-show-command [db]
  (show-command/create-show-command db))

(defn- create-notify-command [db]
  (let [schedule-fn    (fn [options task-fn]
                         (task-scheduler/add-chime scheduler/scheduler
                                                   options
                                                   task-fn))]
    (notify-command/create-notify-command schedule-fn scraper/scraper db)))

(defn- create-find-command [db]
  (find-command/create-find-command scraper/scraper db))

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
  (let [commands   [{:match-fn (matcher/match-first "/notify") 
                     :handle-fn (create-notify-command database/db)}
                    {:match-fn (matcher/match-first "/show")
                     :handle-fn (create-show-command database/db)}
                    {:match-fn (matcher/match-or 
                                (matcher/match-first "/find") 
                                (matcher/match-first-pattern #"^\d*"))
                     :handle-fn (create-find-command database/db)}]
        bot        (bot/create-bot commands)]
    (fn [data ch]
      (bot data (partial send-to-user ch)))))

(defstate bot 
  :start (start-bot))
