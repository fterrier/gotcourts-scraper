(ns gotcourts.bot
  (:require [clj-time.core :as t]
            [clojure.core.async :refer [>!!]]
            [gotcourts
             [bot-parser :as bot-parser]
             [task :as task]]
            [scheduler.scheduler :as scheduler]))

;; (defn- call-and-alert [date old-data extract-fn send-alert-fn]
;;   (when-let [data (extract-fn date)]
;;     (send-alert-fn old-data data)
;;     data))


(defmulti create-tasks
  "Given a command and a list of user tasks, returns a 
   [response deleted-tasks new-tasks] triplet, where a task is
   a {:task-id ... :interval ... :task-fn ... :stop-fn ...} map."
  (fn [{:keys [command]} user-tasks] command))

(defmethod create-tasks :notify [scraper notifier
                                 {:keys [command courts start-time end-time date]} 
                                 user-tasks]
  (let [{:keys [interval extract-fn send-alert-fn]}
        (task/create-gotcourts-task scraper notifier 
                                    courts date (t/minutes 5) start-time end-time)])
  ["Task succesfully added. You will be notified when a court becomes available."
   []
   ])

(defn- get-error-response [error]
  (:text error))

(defn- start-tasks [scheduler tasks] 
  (for [{:keys [interval task-fn] :as task} tasks]
    (assoc task :stop-fn 
           (scheduler/add-chime scheduler interval task-fn))))

(defn handle-command [scheduler scraper notifier tasks {:keys [command error]}]
  (if error [(get-error-response error) tasks]
      (let [[response deleted-tasks new-tasks] 
            (create-tasks scraper notifier command tasks)]
        (doseq [{:keys [stop-fn]} deleted-tasks] (stop-fn))
        (let [new-started-tasks (start-tasks scheduler new-tasks)]
          [response (-> tasks
                        (disj (map :task-id deleted-tasks))
                        (conj (group-by :task-id new-started-tasks)))]))))

(defn- handle-message [scheduler scraper notifier tasks-db data ch]
  (let [{:keys [user] :as message} (bot-parser/parse-message data)
        tasks                      (get @tasks-db user)
        [response new-tasks]       (handle-command scheduler scraper notifier 
                                                   tasks message)]
    (swap! tasks-db assoc user new-tasks)
    (>!! ch response)))

(defn create-bot [scheduler]
  (let [tasks-db (atom {})]
    ;; TODO 
    (partial handle-message scheduler nil nil tasks-db)))

