(ns gotcourts.task
  (:require [clj-time
             [core :as t]
             [format :as f]]
            [gotcourts
             [alerter :as alr]
             [checker :as chk]]
            [clojure.tools.logging :as log]))

(def custom-formatter (f/formatter "YYYY-MM-dd" (t/default-time-zone)))

(defn- venues-with-filtered [data start-time end-time]
  (let [extracted (chk/extract-data data)
        filtered  (chk/filter-free (:courts extracted) start-time end-time)]
    (merge extracted {:courts filtered})))

(defn- fetch-parallel [fetch-fn params-list]
  (->> params-list 
       (pmap (fn [params] [params @(fetch-fn params)]))
       (into {})))

(defn fetch-gotcourts-venues [venue-fn terms]
  "Takes as an input a list of terms and returns a list of matching venue ids."
  (log/info "Extracting venues from gotcourts" terms)
  (let [params-list  (map (fn [term] {:search term}) terms)
        fetched-data (fetch-parallel venue-fn params-list)]
    (->> fetched-data
         (map (fn [[{:keys [search]} data]]
                [search (map #(select-keys % [:id :name]) data)]))
         (into {}))))

(defn fetch-gotcourts-availabilities [availability-fn ids date start-time end-time]
  "A map of tennis clubs, containing courts:
  {id {:name ... :courts [{:free-slots ... :filtered-free-slots ...}]}}"
  (log/info "Extracting availabilities from gotcourts" ids date)
  (let [formatted-date (f/unparse custom-formatter date)
        params-list    (map (fn [id] {:id id :date formatted-date}) ids)
        fetched-data   (fetch-parallel availability-fn params-list)]
    (->> fetched-data
         (map (fn [[{:keys [id]} data]] 
                [id (venues-with-filtered data start-time end-time)]))
         (into {}))))

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

;; TODO move into notify ?
(defn create-gotcourts-task-creator [availability-fn]
  (fn [{:keys [venues date start-time end-time] :as options}]
    {:extract-fn (fn [_] (fetch-gotcourts-availabilities availability-fn venues date start-time end-time))
     :alert-fn (fn [old-data data] (get-alerts old-data data))}))
