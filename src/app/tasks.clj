(ns app.tasks
  (:require [app
             [scraper :as app]]
            [gotcourts.task :as task]
            [mount.core :refer [defstate]]))

(defstate tasks :start 
  [(task/create-gotcourts-task app/scraper)])
