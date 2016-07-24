(ns gotcourts.bot-commands.notify
  (:require [clj-time
             [core :as t]
             [format :as f]]
            [clojure.string :as str]))

;; TODO maybe split parsing from command

(def day-month-year-formatter (f/formatter "dd-MM-yyyy"))

(defn- get-message [{:keys [error success type]}]
  (cond 
    (= error :format-error) {:message "prout"}
    (= success :task-added) {:message "yeee"}))

(defn- number-of-seconds [unparsed-time]
  (try
    (let [ref-time    (f/parse (f/formatters :hour-minute) "00:00")
          parsed-time (f/parse (f/formatters :hour-minute) unparsed-time)]
      (t/in-seconds (t/interval ref-time parsed-time)))
    (catch Exception e)))

(defn- parse-hours [hours-text]
  (let [times      (str/split hours-text #"-")
        start-time (number-of-seconds (first times))
        end-time   (number-of-seconds (second times))]
    (if
      (not= (count times) 2) [nil nil]
      [start-time end-time])))

(defn- parse-int [string]
  (try
    (Integer/parseInt string)
    (catch Exception e)))

(defn- parse-date [string]
  (try
    (f/parse day-month-year-formatter string)
    (catch Exception e)))

(defn- parse-courts [courts-text]
  (let [courts (str/split courts-text #",")]
    (->> courts
         (map parse-int)
         (remove nil?))))

(defn parse-command-chunks [args]
  "/free <courts> <hours> <time span in natural language>"
  (if (not= 3 (count args))
    [nil {:error :format-error :type :args}]
    (let [courts                (parse-courts (first args))
          [start-time end-time] (parse-hours (second args))
          date                  (parse-date (last args))]
      (cond
        (empty? courts) 
        [nil {:error :format-error :type :courts}]
        (or (nil? start-time) (nil? end-time))
        [nil {:error :format-error :type :time}]
        (nil? date)
        [nil {:error :format-error :type :date}]
        :else
        [{:command :add
          :start-time start-time
          :end-time end-time
          :date date
          :courts courts}]))))

(defn- start-tasks [schedule-fn tasks]
  (for [{:keys [interval task-fn] :as task} tasks]
    (assoc task :stop-fn (schedule-fn interval task-fn))))

(defn- get-alert-message [alert]
  {:success :new-alert :options alert})

(defn- extract-and-alert-task-fn [notify-fn extract-fn alert-fn] 
  (fn [date old-data]
    (when-let [data (extract-fn date)]
      (when-let [alerts (alert-fn old-data data)]
        (doseq [alert alerts] 
          (notify-fn (get-alert-message alert))))
      data)))

(defn- add-task-and-respond [create-task-fn notify-fn command]
  (let [{:keys [extract-fn alert-fn]} (create-task-fn command)]
    [{:success :task-added} 
     [{:task-id (dissoc command :command)
       :interval (t/minutes 5)
       :task-fn (extract-and-alert-task-fn notify-fn extract-fn alert-fn)}]]))

(defn- create-tasks-and-response [schedule-fn create-task-fn notify-fn command]
  (let [[response new-tasks] (add-task-and-respond create-task-fn notify-fn command)]
    (let [new-started-tasks (start-tasks schedule-fn new-tasks)]
      [response (merge (->> new-started-tasks
                            (group-by :task-id)
                            (map (fn [[id tasks]] [id (first tasks)]))
                            (into {})))])))

(defn- handle-message* [schedule-fn create-task-fn tasks-db user args send-to-user-fn]
  (let [[command error]      (parse-command-chunks args)]
    (if error (send-to-user-fn error)
        (let [tasks                (get @tasks-db user)
              notify-fn            (fn [response] (send-to-user-fn response)) 
              [response new-tasks] (create-tasks-and-response schedule-fn 
                                                              create-task-fn 
                                                              notify-fn 
                                                              command)]
          (swap! tasks-db assoc user new-tasks)
          (send-to-user-fn 
           (assoc response :text (get-message response)))))))

(defn create-notify-command [schedule-fn create-task-fn tasks-db]
  (partial handle-message* schedule-fn create-task-fn tasks-db))


;; (defmethod handle-command :delete-all [_ _ _ user-tasks]
;;   [{:success :task-deleted-all}
;;    (map (fn [[_ task]] task) user-tasks)
;;    []])

;;    (doseq [{:keys [stop-fn]} deleted-tasks] (stop-fn))

