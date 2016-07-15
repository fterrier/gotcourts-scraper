(ns app.tasks
  (:require [app.scraper :as app]
            [clj-time.core :as t]
            [gotcourts.task :as task]
            [mount.core :refer [defstate]]))

;; (defn- call-and-alert [date old-data extract-fn send-alert-fn]
;;   (try 
;;     (when-let [data (extract-fn date)]
;;       (send-alert-fn old-data data)
;;       data)
;;     (catch Exception e 
;;       (log/error "Exception trying to execute task" e))))

(defstate tasks :start 
  [(task/create-gotcourts-task app/scraper println 
                               [15 16 17] (t/minutes 5) 50400 60000)])
