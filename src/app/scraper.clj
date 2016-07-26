(ns app.scraper
  (:require [gotcourts.scraper :as scraper]
            [mount.core :refer [defstate]]
            [clojure.tools.logging :as log]))

(defstate scraper :start 
  (scraper/gotcourts-scraper))
