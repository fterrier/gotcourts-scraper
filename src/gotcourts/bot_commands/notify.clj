(ns gotcourts.bot-commands.notify
  (:require [clj-time.core :as t]
            [clojure.tools.logging :as log]
            [gotcourts.bot-commands
             [notify-parser :as notify-parser]
             [notify-message :as notify-message]]))

(defn- start-tasks [schedule-fn tasks]
  (for [{:keys [interval task-fn] :as task} tasks]
    (assoc task :stop-fn (schedule-fn interval task-fn))))

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

(defn- add-task-and-respond [create-task-fn notify-fn command]
  (let [{:keys [extract-fn alert-fn]} (create-task-fn command)]
    [{:success :task-added :options command}
     [{:task-id (dissoc command :command)
       :interval (t/minutes 5)
       :task-fn (extract-and-alert-task-fn (:date command) 
                                           notify-fn extract-fn alert-fn)}]]))

(defn- create-tasks-and-response [schedule-fn create-task-fn notify-fn command]
  (let [[response new-tasks] (add-task-and-respond create-task-fn 
                                                   notify-fn 
                                                   command)
        new-started-tasks    (start-tasks schedule-fn new-tasks)]
    [response (merge (->> new-started-tasks
                          (group-by :task-id)
                          (map (fn [[id tasks]] [id (first tasks)]))
                          (into {})))]))

(defn- handle-message* [schedule-fn create-task-fn tasks-db user args send-to-user-fn]
  (let [[command error] (notify-parser/parse-command-chunks args)]
    (if error (send-to-user-fn (assoc error 
                                      :text (notify-message/get-message error)))
        (let [tasks                (get @tasks-db user)
              notify-fn            (fn [response] 
                                     (send-to-user-fn 
                                      (assoc response :text
                                       (notify-message/get-message response)))) 
              [response new-tasks] (create-tasks-and-response schedule-fn 
                                                              create-task-fn 
                                                              notify-fn 
                                                              command)]
          (swap! tasks-db assoc user new-tasks)
          (send-to-user-fn
           (assoc response :text (notify-message/get-message response)))))))

(defn create-notify-command [schedule-fn create-task-fn tasks-db]
  (partial handle-message* schedule-fn create-task-fn tasks-db))


;; (defmethod handle-command :delete-all [_ _ _ user-tasks]
;;   [{:success :task-deleted-all}
;;    (map (fn [[_ task]] task) user-tasks)
;;    []])

;;    (doseq [{:keys [stop-fn]} deleted-tasks] (stop-fn))

