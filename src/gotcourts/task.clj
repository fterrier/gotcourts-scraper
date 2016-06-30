(ns gotcourts.task
  (:require [clj-time
             [core :as t]
             [format :as f]]
            [gotcourts
             [alerter :as alr]
             [checker :as chk]]))

(def custom-formatter (f/formatter "YYYY-MM-dd"))


(defn- scrape-data [ids]
  )

(defn extract-gotcourts [scraper ids date start-time end-time]
  (let [data @(scraper {:id ids :date (f/unparse custom-formatter date)})
        extracted (chk/extract-data data)
        filtered (chk/filter-free (:courts extracted) start-time end-time)]
    filtered))

(defn handle-alerts [old-data data]
  (let [alerts (alr/get-alerts old-data data)]
    (println alerts)))

(defn create-gotcourts-task [scraper ;ids interval start-time end-time
                             ]
  {:interval (-> 5 t/seconds)
   :extract-fn (fn [_] (extract-gotcourts scraper 17 (t/now) 50000 51000))
   :send-alert-fn (fn [old-data data] (handle-alerts old-data data))})
