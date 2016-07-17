(ns gotcourts.bot-parser
  (:require [clojure.string :as str]
            [clj-time
             [core :as t]
             [format :as f]]))

(defn- number-of-seconds [unparsed-time]
  (let [ref-time    (f/parse (f/formatters :hour-minute) "00:00")
        parsed-time (f/parse (f/formatters :hour-minute) unparsed-time)]
    (t/in-seconds (t/interval ref-time parsed-time))))

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

(defn- parse-courts [courts-text]
  (let [courts (str/split courts-text #",")]
    (->> courts
         (map parse-int)
         (remove nil?))))

(defmulti parse-command-chunks 
  (fn [command args] command))

(def day-month-year-formatter (f/formatter "dd-MM-yyyy"))

(defmethod parse-command-chunks "/notify" [command args]
  "/free <courts> <hours> <time span in natural language>"
  (if (not= 3 (count args))
    [nil {:error :format-error :type :args}]
    (let [courts                (parse-courts (first args))
          [start-time end-time] (parse-hours (second args))
          date                  (f/parse day-month-year-formatter (last args))]
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

(defmethod parse-command-chunks "/show" [command args]
  [{:command :show}])

(defmethod parse-command-chunks :default [command args]
  [nil {:error :command-not-found}])

(defn- parse-command [text]
  (let [[command & args] (str/split (str/trim text) #" ")]
    (parse-command-chunks command args)))

;; TODO handle edited-message
(defn parse-message [{:keys [message] :as data}]
  "Takes a telegram message and parses it to {:user ... :command ... :error ...}"
  (let [user            (:from message)
        [command error] (parse-command (:text message))]
    {:user    user
     :command command
     :error   error}))
