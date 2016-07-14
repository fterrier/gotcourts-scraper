(ns app.tasks
  (:require [app.scraper :as app]
            [clj-time.core :as t]
            [gotcourts.task :as task]
            [mount.core :refer [defstate]]))

(defstate tasks :start 
  [(task/create-gotcourts-task app/scraper println 
                               [15 16 17] (t/minutes 5) 50400 60000)])
