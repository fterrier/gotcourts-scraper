(ns gotcourts.task
  (:require [clj-time
             [core :as t]
             [format :as f]]
            [gotcourts
             [alerter :as alr]
             [checker :as chk]]
            [clojure.tools.logging :as log]))

(def custom-formatter (f/formatter "YYYY-MM-dd" (t/default-time-zone)))

(defn- scrape-data [scraper id formatted-date start-time end-time]
  (let [data      @(scraper {:id id :date formatted-date})
        extracted (chk/extract-data data)
        filtered  (chk/filter-free (:courts extracted) start-time end-time)]
    (merge extracted {:courts filtered})))

(defn extract-gotcourts [scraper ids date start-time end-time]
  "A map of tennis clubs, containing courts:
  {id {:name ... :courts [{:free-slots ... :filtered-free-slots ...}]}}"
  (log/info "Extracting from gotcourts" ids date)
  (->> ids
       (pmap (fn [id] [id (scrape-data scraper id
                                       (f/unparse custom-formatter date)
                                       start-time end-time)]))
       (into {})))

(defn- get-alert-data [id old-data data]
  {:alerts (alr/get-alerts (get-in old-data [id :courts]) (get-in data [id :courts]))
   :venue  (get data id)})

(defn get-alerts [old-data data]
  "Takes as an input 2 maps of 
   {<venue-id> {:name ... :courts [{:filtered-free-slots} ...]}] 
   and returns a map of 
   {<venue-id> {:alerts [...]} :venue {:name ... :courts ...}}"
  (let [ids        (into #{} (concat (keys old-data) (keys data)))
        alert-data (->> ids 
                        (map (fn [id] [id (get-alert-data id old-data data)]))
                        (remove (fn [[id alert-data]] (empty? (:alerts alert-data))))
                        (into {}))]
    alert-data))

(defn create-gotcourts-task-creator [scraper]
  (fn [{:keys [venues date start-time end-time] :as options}]
    {:extract-fn (fn [_] (extract-gotcourts scraper venues date start-time end-time))
     :alert-fn (fn [old-data data] (get-alerts old-data data))}))
