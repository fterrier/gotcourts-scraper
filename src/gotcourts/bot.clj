(ns gotcourts.bot
  (:require [clj-time
             [core :as t]]
            [clojure.core.async :refer [>!!]]
            [gotcourts.bot-parser :as bot-parser]
            [gotcourts.bot-messages :as bot-messages]))

(defn- get-alert-message [alert]
  {:success :new-alert :options alert})

(defn- extract-and-alert-task-fn [notify-fn extract-fn alert-fn] 
  (fn [date old-data]
    (when-let [data (extract-fn date)]
      (when-let [alerts (alert-fn old-data data)]
        (doseq [alert alerts] 
          (notify-fn (get-alert-message alert))))
      data)))

(defmulti handle-command
  "Given a command and a list of user tasks, returns a 
   [response deleted-tasks new-tasks] triplet, where a task is
   a {:task-id ... :interval ... :task-fn ... :stop-fn ...} map."
  (fn [create-task-fn notify-fn {:keys [command]} user-tasks] command))

(defmethod handle-command :add [create-task-fn notify-fn command _]
  (let [{:keys [extract-fn alert-fn]} (create-task-fn command)]
    [{:success :task-added} 
     []
     [{:task-id (dissoc command :command)
       :interval (t/minutes 5)
       :task-fn (extract-and-alert-task-fn notify-fn extract-fn alert-fn)}]]))

(defmethod handle-command :delete-all [_ _ _ user-tasks]
  [{:success :task-deleted-all}
   (map (fn [[_ task]] task) user-tasks)
   []])

(defmethod handle-command :show [_ _ _ user-tasks]
  [{:success :show :options user-tasks} [] []])

(defmethod handle-command :default [_ _ _ _]
  [{:error :method-not-recognized} [] []])

(defn- start-tasks [schedule-fn tasks]
  (for [{:keys [interval task-fn] :as task} tasks]
    (assoc task :stop-fn (schedule-fn interval task-fn))))

(defn create-tasks-and-response [schedule-fn create-task-fn
                      notify-fn tasks {:keys [command error]}]
  (if error 
    [error tasks]
    (let [[response deleted-tasks new-tasks]
          (handle-command create-task-fn notify-fn command tasks)]
      (doseq [{:keys [stop-fn]} deleted-tasks] (stop-fn))
      (let [new-started-tasks (start-tasks schedule-fn new-tasks)]
        [response (-> (apply dissoc tasks (map :task-id deleted-tasks))
                      (merge (->> new-started-tasks
                                  (group-by :task-id)
                                  (map (fn [[id tasks]] [id (first tasks)]))
                                  (into {}))))]))))

(defn- send-to-user [ch response]
  (>!! ch (bot-messages/unparse response)))

(defn- handle-message* [schedule-fn create-task-fn tasks-db data ch]
  (let [{:keys [user] :as message} (bot-parser/parse-message data)
        tasks                      (get @tasks-db user)
        notify-fn                  (fn [response] (send-to-user ch response)) 
        [response new-tasks]       (create-tasks-and-response schedule-fn 
                                                   create-task-fn 
                                                   notify-fn 
                                                   tasks message)]
    (swap! tasks-db assoc user new-tasks)
    (send-to-user ch response)))

(defn create-bot [schedule-fn create-task-fn]
  "Creates a bot that creates and schedules task, and notifies the user based on
   the alerts sent by those tasks.
   - schedule-fn: a function that takes an interval and a task-fn
   - create-task-fn: a function that takes a notifier and options as an 
     argument and that returns a task {:extract-fn ... :alert-fn ...}
   A bot is a function that takes a [data ch] as a parameter, where:
   - data: the data sent by the client
   - ch: a way to talk back to the client"
  ;; TODO DB should be outside
  (let [tasks-db (atom {})]
    (partial handle-message* schedule-fn create-task-fn tasks-db)))

