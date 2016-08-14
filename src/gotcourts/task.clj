(ns gotcourts.task
  (:require [clj-time
             [core :as t]
             [format :as f]]
            [gotcourts
             [alerter :as alr]
             [checker :as chk]]
            [clojure.tools.logging :as log]))

;; TODO integrate this file in the scraper maybe as scraping-service ?

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
  "Takes as an input a list of terms and returns a list of matching venues 
   {:id ... :name ...}."
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

