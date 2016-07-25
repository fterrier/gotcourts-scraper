(ns app.scraper
  (:require [gotcourts.scraper :as scraper]
            [mount.core :refer [defstate]]
            [clojure.tools.logging :as log]))

(defstate scraper :start 
  (let [scraper (scraper/gotcourts-scraper)]
    (fn [params] 
      (scraper/fetch-availabilities scraper params))))
