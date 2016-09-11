(ns gotcourts.bot-commands.message
  (:require [clj-time
             [core :as t]
             [format :as f]]
            [clojure.string :as str]))

(def human-date-formatter (f/formatter "EEE, dd MMM yyyy" (t/default-time-zone)))

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

(defn- get-new-alerts-message [{:keys [alerts-venue-map new-venue date]}]
  (let [{:keys [alerts venue]} alerts-venue-map
        new-slots              (filter-alerts-slots alerts :new-slot)
        gone-slots             (filter-alerts-slots alerts :gone-slot)
        formatted-date         (f/unparse human-date-formatter date)]
    {:message
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
                   (map #(get-slot-text (:courts venue) %) gone-slots)))
          "\n\nType /notify to get notified as soon as a court becomes available.")
     :options {:parse-mode :markdown}}))

(defn- chosen-venue-names [chosen-venues]
  (map (fn [[_ venues]] (:name (first venues))) chosen-venues))

(defn- get-task-added-message [{:keys [date time chosen-venues]}]
  (let [[start-time end-time] time]
    {:message
     (str "Got it, will notify you as soon as a court is available at "
          (str/join ", " (chosen-venue-names chosen-venues)) " on "
          (f/unparse human-date-formatter date) " between "
          (from-seconds start-time) " and " (from-seconds end-time) ".")}))

(defn- get-no-alerts-message [{:keys [date time chosen-venues]}]
  (let [formatted-date (f/unparse human-date-formatter date)]
    {:message 
     (str "The venue " (str/join ", " (chosen-venue-names chosen-venues)) 
          " has no courts available on " formatted-date 
          " in the time you requested.\n\nType /notify to get notified as soon as a court becomes available.")
     :options {:parse-mode :markdown}}))

(defn- get-ambiguous-venue-message [{:keys [date ambiguous-venues message-id]}]
  "- ambiguous-venues : [<term> [<venue>, ...]"
  (let [[term venues] ambiguous-venues]
    {:message
     (str "Your search for " term " matched more than one venue. Please select one below:")
     :options {:parse-mode :markdown
               :message-id message-id
               :reply-keyboard (map-indexed (fn [index elem] 
                                              [{:text (str (inc index) ". " (:name elem))}]) venues)}}))

(defn- get-no-command-history-message [_]
  {:message
   (str "We did not find any /find commands in your history. Please first search for courts using /find <courts> <time> <date>.")})

(defn get-message [{:keys [success error type options]}]
  (cond 
    (= success :task-added)
    (get-task-added-message options)
    (= success :new-alert)
    (get-new-alerts-message options)
    (= success :no-alerts)
    (get-no-alerts-message options)
    (= success :ambiguous-venue)
    (get-ambiguous-venue-message options)
    (= error :no-command-history)
    (get-no-command-history-message options)))

(defn assoc-message [response]
  (assoc response :text (get-message response)))
