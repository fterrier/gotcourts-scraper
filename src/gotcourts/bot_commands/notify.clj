(ns gotcourts.bot-commands.notify
  (:require [clj-time.core :as t]
            [clojure.tools.logging :as log]
            [gotcourts
             [scraper :as scraper]
             [task :as task]]
            [bot.command-parser :as parser]
            [gotcourts.bot-commands
             [notify-message :as notify-message]]))

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

(defn- parse-command-chunks [args]
  (let [format-type-args    [[:venues :list]
                             [:time :timespan]
                             [:date :date]]
        format-args         (map second format-type-args)
        type-args           (map first format-type-args)
        [parsed-args error] (parser/parse-command-chunks format-args args)]
    (if error
      [nil error]
      (reduce (fn [[command error] [type value]]
                (if (nil? value) 
                  (reduced [nil {:error :format-error :type type}])
                  [(assoc command type value) nil]))
              [{} nil] (map vector type-args parsed-args)))))

(defn- handle-message* [schedule-fn scraper tasks-db user args send-to-user-fn]
  (let [[command error] (parse-command-chunks args)]
    (if error (send-to-user-fn (assoc error :text (notify-message/get-message error)))
        (let [tasks                (get @tasks-db user)
              notify-fn            (fn [response] 
                                     (send-to-user-fn 
                                      (assoc response :text
                                       (notify-message/get-message response)))) 
              [response new-tasks] (create-tasks-and-response schedule-fn 
                                                              scraper
                                                              notify-fn 
                                                              command)]
          (swap! tasks-db assoc-in [user :tasks] new-tasks)
          (send-to-user-fn
           (assoc response :text (notify-message/get-message response)))))))

(defn create-notify-command [schedule-fn scraper tasks-db]
  (partial handle-message* schedule-fn scraper tasks-db))

