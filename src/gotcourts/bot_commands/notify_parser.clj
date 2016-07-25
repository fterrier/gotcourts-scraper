(ns gotcourts.bot-commands.notify-parser
  (:require [clj-time
             [core :as t]
             [format :as f]]
            [clojure.string :as str]))

(def dmy-1 (f/formatter "dd-MM-yyyy"))
(def dmy-2 (f/formatter "dd/MM/yyyy"))

(def hour-minute (f/formatter (t/default-time-zone) 
                              "HH'h'" "HH'h'mm" "HH:mm" "HHmm"))

(defn- parse-time-nil [formatter unparsed-time]
  (try
    (f/parse formatter unparsed-time)
    (catch Exception e)))

(defn- parse-time [formatters unparsed-time]
  "Try the formatters in order until one does not return nil and returns that value."
  (reduce #(let [parsed-time (parse-time-nil %2 unparsed-time)]
             (if parsed-time (reduced parsed-time) nil)) nil formatters))

(defn- number-of-seconds [unparsed-time]
  (let [ref-time    (f/parse hour-minute "00:00")
        parsed-time (parse-time [hour-minute] unparsed-time)]
    (if-not parsed-time nil
      (t/in-seconds (t/interval ref-time parsed-time)))))

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

(defn- parse-known-expressions [string]
  ;; TODO use duckling
  nil)

(defn- parse-date [string]
  (println string)
  (let [parsed-time (parse-time [dmy-1 dmy-2] string)]
    (if parsed-time parsed-time
        (parse-known-expressions string))))

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
          date                  (parse-date (str/join " " (drop 2 args)))]
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
