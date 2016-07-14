(ns gotcourts.task
  (:require [clj-time
             [core :as t]
             [format :as f]]
            [gotcourts
             [alerter :as alr]
             [checker :as chk]]))

(def custom-formatter (f/formatter "YYYY-MM-dd"))

(defn- scrape-data [scraper id formatted-date start-time end-time]
  (let [data      @(scraper {:id id :date formatted-date})
        extracted (chk/extract-data data)
        filtered  (chk/filter-free (:courts extracted) start-time end-time)]
    (merge extracted {:courts filtered})))

(defn extract-gotcourts [scraper ids date start-time end-time]
  "A map of tennis clubs, containing courts:
  {id {:name ... :courts [{:free-slots ... :filtered-free-slots ...}]}}"
  (->> ids
       (pmap (fn [id] [id (scrape-data scraper id
                                       (f/unparse custom-formatter date)
                                       start-time end-time)]))
       (into {})))

(defn- get-alert-data [id old-data data]
  {:alerts (alr/get-alerts (get-in old-data [id :courts]) (get-in data [id :courts]))
   :data   (get data id)})

(defn handle-alerts [notifier old-data data]
  "Generates a alert diff from an old map of tennis clubs to a new one and sends
   a notification in case there is a new alert."
  (let [ids        (into #{} (concat (keys old-data) (keys data)))
        alert-data (->> ids 
                        (map (fn [id] [id (get-alert-data id old-data data)]))
                        (remove (fn [[id alert-data]] (empty? (:alerts alert-data))))
                        (into {}))]
    (println alert-data)
    (notifier alert-data)))

(defn create-gotcourts-task [scraper notifier ids interval start-time end-time]
  {:interval interval
   :extract-fn (fn [_] (extract-gotcourts scraper ids (t/now) start-time end-time))
   :send-alert-fn (fn [old-data data] (handle-alerts notifier old-data data))})
