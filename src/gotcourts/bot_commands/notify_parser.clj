(ns gotcourts.bot-commands.notify-parser
  (:require [clj-time
             [core :as t]
             [format :as f]]
            [clojure.string :as str]))

(def day-month-year-formatter (f/formatter "dd-MM-yyyy"))

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

(defn- parse-venues [venues-text]
  (let [venues (str/split venues-text #",")]
    (->> venues
         (map parse-int)
         (remove nil?))))

(defn parse-command-chunks [args]
  "/free <venues> <hours> <time span in natural language>"
  (if (not= 3 (count args))
    [nil {:error :format-error :type :args}]
    (let [venues                (parse-venues (first args))
          [start-time end-time] (parse-hours (second args))
          date                  (parse-date (last args))]
      (cond
        (empty? venues) 
        [nil {:error :format-error :type :venues}]
        (or (nil? start-time) (nil? end-time))
        [nil {:error :format-error :type :time}]
        (nil? date)
        [nil {:error :format-error :type :date}]
        :else
        [{:command :add
          :start-time start-time
          :end-time end-time
          :date date
          :venues venues}]))))
