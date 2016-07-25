(ns gotcourts.bot-commands.notify-message
  (:require [clj-time.core :as t]
            [clj-time.format :as f]))

(def human-date-formatter (f/formatter "EEE, dd MMM yyyy" (t/default-time-zone)))

(def format-message 
  "Use format /notify <courts> <time> <date>. For example, to get an alert when a court becomes available:
   - /notify 15,14 15:00-17:00 27-11-2016")

(defn- get-format-error-message [type]
  (str
   (case type
     :args   "Wrong number of arguments."
     :venues "Wrong format for venues."
     :time   "Wrong format for time."
     :date   "Wrong format for date.")
   " " format-message))

(defn- filter-alerts-slots [alerts filter-type]
  (->> alerts
       (filter (fn [[type slots]] (= type filter-type)))
       (map (fn [[type slots]] slots))))

(defn- find-court [courts id]
  (some #(when (= (:id %) id) %) courts))

(defn- from-seconds [seconds]
  (let [ref-time (f/parse (f/formatters :hour-minute) "00:00")
        time     (t/plus ref-time (t/seconds seconds))]
    (f/unparse (f/formatters :hour-minute) time)))

(defn- get-slot-text [courts {:keys [slot id]}]
  (let [court (find-court courts id)]
    (str " - " (from-seconds (:startTime slot)) "-" (from-seconds (:endTime slot)) 
         ": " (:label court) "\n")))

(defn- get-new-alerts-message [{:keys [alerts-per-venue new-venue date]}]
  (let [[_ {:keys [alerts venue]}] alerts-per-venue
        new-slots                  (filter-alerts-slots alerts :new-slot)
        gone-slots                 (filter-alerts-slots alerts :gone-slot)
        formatted-date             (f/unparse human-date-formatter date)]
    (str (if new-venue 
           (str "The venue " (:name venue) 
                " has courts available on " formatted-date
                " in the time you requested.\n")
           (str "The venue " (:name venue) 
                " has some changes on " formatted-date
                " in the time you requested.\n"))
         "\n"
     (when-not (empty? new-slots)
       (apply str "The following slots are available:\n"
              (map #(get-slot-text (:courts venue) %) new-slots)))
     (when (and (not (empty? gone-slots)) (not new-venue))
       (apply str "The following slots are not available any more:\n" 
              (map #(get-slot-text (:courts venue) %) gone-slots))))))

(defn- get-task-added-message [{:keys [date start-time end-time]}]
  "Task added successfuly. You will be notified as soon as a court becomes available.")

(defn get-message [{:keys [error success type options]}]
  (cond 
    (= error :format-error) 
    {:message (get-format-error-message type) 
     :options {:parse-mode :markdown}}
    (= success :task-added) 
    {:message (get-task-added-message options)}
    (= success :new-alert) 
    {:message (get-new-alerts-message options) 
     :options {:parse-mode :markdown}}))
