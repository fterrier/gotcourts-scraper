(ns app.scraper
  (:require [gotcourts.scraper :as scraper]
            [mount.core :refer [defstate]]
            [clojure.tools.logging :as log]))

;; TODO remove this state
(defstate scraper :start 
  (let [scraper (scraper/gotcourts-scraper)]
    (fn [params] 
      (scraper/retrieve-raw-data scraper params))))
